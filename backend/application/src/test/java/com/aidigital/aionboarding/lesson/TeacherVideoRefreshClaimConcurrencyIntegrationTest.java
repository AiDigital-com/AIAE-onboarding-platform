package com.aidigital.aionboarding.lesson;

import com.aidigital.aionboarding.domain.common.dictionary.DictionaryEntity;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins P7's third concurrency contract: {@link LessonRepository#claimForTeacherVideoRefresh}
 * lets at most one of two callers racing to refresh the same lesson's teacher-video status
 * proceed to call HeyGen — the other observes {@code 0} rows updated and must return the
 * lesson unchanged (see {@code TeacherVideoRefreshServiceImplTest} for the unit-level proof that
 * {@code TeacherVideoRefreshServiceImpl} actually skips the HeyGen call and the save when the
 * claim is lost; this test proves the claim primitive itself is atomic under real concurrent
 * database access, which a Mockito test cannot).
 * <p>
 * Unlike the YouTube-backfill and abandoned-upload claims, this claim is a plain
 * {@code UPDATE ... WHERE version = ?} with no PostgreSQL-specific syntax, so this H2 proof
 * (PostgreSQL compatibility mode, {@code application-test.yml}) is a full proof and not merely
 * a best-effort one — no Testcontainers/Docker needed.
 */
@SpringBootTest
@ActiveProfiles("test")
class TeacherVideoRefreshClaimConcurrencyIntegrationTest {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void twoConcurrentClaimsAtTheSameVersionShouldLetExactlyOneWinTest() throws Exception {
		// Given: one lesson at version 0, claimed by two callers that both read that version
		// before either attempts to claim — simulating two nodes racing to refresh it. The
		// version comes from the save() return value, not a reload, and the final check below
		// selects only the version column via a scalar JPQL query — both deliberately avoid
		// reloading the full Lesson entity through a fresh Hibernate session: doing so was
		// found, separately from this test's own concern, to throw a Jackson deserialize
		// error against this H2 instance's JSON column type even for an empty generationMetadata
		// map — a real H2-test-environment gap, not a bug in the claim primitive this test
		// exists to prove, and not reproduced against real PostgreSQL. Recorded in
		// docs/aiae-migration-log.md P7; not this phase's to fix.
		Lesson savedLesson = persistLesson();
		Long lessonId = savedLesson.getId();
		Long observedVersion = savedLesson.getVersion();

		// Both callables start from the same released latch rather than a barrier held across
		// the claim: a claim is an UPDATE, which — correctly — blocks on the row lock instead of
		// skipping it, unlike the two SKIP LOCKED claims elsewhere in P7. Holding a barrier
		// across it (tried first) deadlocked: the loser's UPDATE blocks waiting for the winner's
		// row lock, so it never reaches a barrier the winner is waiting on. Naturally serialized
		// blocking is exactly the mechanism that makes the claim atomic, so this test exercises
		// it rather than fighting it.
		CountDownLatch bothReady = new CountDownLatch(2);
		Callable<Integer> claim = () -> {
			TransactionTemplate transaction = new TransactionTemplate(transactionManager);
			bothReady.countDown();
			bothReady.await(10, TimeUnit.SECONDS);
			return transaction.execute(status -> lessonRepository.claimForTeacherVideoRefresh(lessonId, observedVersion));
		};

		// When: both claims are submitted together; whichever's UPDATE acquires the row lock
		// first commits and releases it, and the other's UPDATE — which was blocked waiting for
		// that exact lock — then proceeds and correctly matches zero rows.
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> nodeA = pool.submit(claim);
			Future<Integer> nodeB = pool.submit(claim);
			int rowsUpdatedByA = nodeA.get(10, TimeUnit.SECONDS);
			int rowsUpdatedByB = nodeB.get(10, TimeUnit.SECONDS);

			// Then: exactly one claim won (1 row updated) and the other lost (0 rows updated) —
			// never both winning (which would mean two HeyGen calls) and never both losing
			// (which would mean neither node ever refreshes).
			assertThat(rowsUpdatedByA + rowsUpdatedByB).isEqualTo(1);
			assertThat(List.of(rowsUpdatedByA, rowsUpdatedByB)).containsExactlyInAnyOrder(0, 1);

			// And: the version advanced by exactly one, not two — the loser never touched it.
			Long finalVersion = readVersion(lessonId);
			assertThat(finalVersion).isEqualTo(observedVersion + 1);
		} finally {
			pool.shutdownNow();
		}
	}

	/**
	 * Reads only the {@code version} column via a scalar JPQL query, deliberately avoiding a
	 * full entity load (see the note in the test above).
	 */
	private Long readVersion(Long lessonId) {
		return entityManager.createQuery("SELECT l.version FROM Lesson l WHERE l.id = :id", Long.class)
				.setParameter("id", lessonId)
				.getSingleResult();
	}

	private Lesson persistLesson() {
		LessonStatus statusEntity = lessonStatusRepository.save(dictionary(LessonStatus::new, "status-tvr-concurrency"));
		LessonPublicationStatus publicationStatus =
				lessonPublicationStatusRepository.save(dictionary(LessonPublicationStatus::new, "pub-tvr-concurrency"));
		LessonContentFormat contentFormat =
				lessonContentFormatRepository.save(dictionary(LessonContentFormat::new, "format-tvr-concurrency"));

		Lesson lesson = new Lesson();
		lesson.setTitle("Concurrency Test Lesson");
		lesson.setDescription("description");
		lesson.setStatus(statusEntity);
		lesson.setUserInstructions("");
		lesson.setDepth("standard");
		lesson.setTone("clear");
		lesson.setDesiredFormat("structured");
		lesson.setContentFormat(contentFormat);
		lesson.setContentMarkdown("content");
		lesson.setContentHtml("<p>content</p>");
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		// Deliberately empty, not a nested teacherVideo map: this test proves the claim
		// primitive's atomicity, not the JSON metadata round trip covered elsewhere.
		lesson.setGenerationMetadata(java.util.Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(List.of());
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lessonRepository.save(lesson);
	}

	private <T extends DictionaryEntity> T dictionary(Supplier<T> factory, String code) {
		T entity = factory.get();
		entity.setCode(code);
		entity.setName(code);
		entity.setDisplayOrder(1);
		entity.setIsActive(true);
		return entity;
	}
}
