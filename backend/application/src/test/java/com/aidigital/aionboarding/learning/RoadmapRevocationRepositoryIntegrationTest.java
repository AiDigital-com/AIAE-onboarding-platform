package com.aidigital.aionboarding.learning;

import com.aidigital.aionboarding.domain.common.dictionary.LessonContentFormatCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.repositories.UserLessonRepository;
import com.aidigital.aionboarding.domain.learning.repositories.UserRoadmapRepository;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapLessonRepository;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the set-based bulk roadmap-revocation queries against real
 * PostgreSQL: {@link UserLessonRepository#deleteRoadmapDerivedLessonEnrollments} must retain a
 * lesson enrollment only when another roadmap the same user remains enrolled in also grants it,
 * and {@link UserRoadmapRepository#deleteByUserIdsAndRoadmapId} must remove only the targeted
 * roadmap's enrollment rows.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RoadmapRevocationRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private UserLessonRepository userLessonRepository;

	@Autowired
	private UserRoadmapRepository userRoadmapRepository;

	@Autowired
	private RoadmapRepository roadmapRepository;

	@Autowired
	private RoadmapLessonRepository roadmapLessonRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	@Test
	void deleteRoadmapDerivedLessonEnrollmentsShouldKeepLessonsStillGrantedByAnotherRoadmapTest() {
		// Given: roadmapA has lessons L1 (shared with roadmapB) and L2 (only in roadmapA)
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User userWithSharedRoadmap = userRepository.save(user(role, "shared@test.com"));
		User userWithOnlyRevokedRoadmap = userRepository.save(user(role, "only@test.com"));

		Lesson lessonL1 = lessonRepository.save(lesson("L1"));
		Lesson lessonL2 = lessonRepository.save(lesson("L2"));
		Roadmap roadmapA = roadmapRepository.save(roadmap("Roadmap A"));
		Roadmap roadmapB = roadmapRepository.save(roadmap("Roadmap B"));
		roadmapLessonRepository.save(roadmapLesson(roadmapA, lessonL1, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmapA, lessonL2, 1));
		roadmapLessonRepository.save(roadmapLesson(roadmapB, lessonL1, 0));

		userRoadmapRepository.save(userRoadmap(userWithSharedRoadmap, roadmapA));
		userRoadmapRepository.save(userRoadmap(userWithSharedRoadmap, roadmapB));
		userRoadmapRepository.save(userRoadmap(userWithOnlyRevokedRoadmap, roadmapA));

		userLessonRepository.save(userLesson(userWithSharedRoadmap, lessonL1));
		userLessonRepository.save(userLesson(userWithSharedRoadmap, lessonL2));
		userLessonRepository.save(userLesson(userWithOnlyRevokedRoadmap, lessonL1));
		userLessonRepository.save(userLesson(userWithOnlyRevokedRoadmap, lessonL2));

		// When: bulk-revoking roadmapA for both users in one statement
		int deletedCount = userLessonRepository.deleteRoadmapDerivedLessonEnrollments(
				List.of(userWithSharedRoadmap.getId(), userWithOnlyRevokedRoadmap.getId()), roadmapA.getId());

		// Then: the shared-roadmap user keeps L1 (still granted by roadmapB) but loses L2;
		// the other user has no other roadmap granting either lesson, so loses both
		assertThat(deletedCount).isEqualTo(3);
		assertThat(userLessonRepository.findByUserId(userWithSharedRoadmap.getId()))
				.extracting(ul -> ul.getId().getLessonId())
				.containsExactly(lessonL1.getId());
		assertThat(userLessonRepository.findByUserId(userWithOnlyRevokedRoadmap.getId())).isEmpty();
	}

	@Test
	void deleteByUserIdsAndRoadmapIdShouldRemoveOnlyTheTargetedRoadmapEnrollmentTest() {
		// Given: one user enrolled in two roadmaps
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User learner = userRepository.save(user(role, "learner@test.com"));
		Roadmap roadmapA = roadmapRepository.save(roadmap("Roadmap A"));
		Roadmap roadmapB = roadmapRepository.save(roadmap("Roadmap B"));
		userRoadmapRepository.save(userRoadmap(learner, roadmapA));
		userRoadmapRepository.save(userRoadmap(learner, roadmapB));

		// When: bulk-revoking only roadmapA
		int deletedCount = userRoadmapRepository.deleteByUserIdsAndRoadmapId(List.of(learner.getId()),
				roadmapA.getId());

		// Then: only the roadmapA enrollment row is gone
		assertThat(deletedCount).isEqualTo(1);
		assertThat(userRoadmapRepository.findByIdUserId(learner.getId()))
				.extracting(ur -> ur.getId().getRoadmapId())
				.containsExactly(roadmapB.getId());
	}

	private User user(UserRole role, String email) {
		User user = new User();
		user.setName(email);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Roadmap roadmap(String title) {
		Roadmap roadmap = new Roadmap();
		roadmap.setTitle(title);
		roadmap.setDescription("description");
		roadmap.setTags(List.of());
		roadmap.setCreatedBy("tester");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		roadmap.setCreatedAt(now);
		roadmap.setUpdatedAt(now);
		return roadmap;
	}

	private Lesson lesson(String title) {
		LessonStatus status = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow();
		LessonPublicationStatus publicationStatus =
				lessonPublicationStatusRepository.findByCode(LessonPublicationStatusCode.PUBLISHED).orElseThrow();
		LessonContentFormat contentFormat =
				lessonContentFormatRepository.findByCode(LessonContentFormatCode.MARKDOWN).orElseThrow();
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

		Lesson lesson = new Lesson();
		lesson.setTitle(title);
		lesson.setDescription("description");
		lesson.setStatus(status);
		lesson.setUserInstructions("");
		lesson.setDepth("standard");
		lesson.setTone("clear");
		lesson.setDesiredFormat("structured theoretical lesson");
		lesson.setContentFormat(contentFormat);
		lesson.setContentMarkdown("content");
		lesson.setContentHtml("<p>content</p>");
		lesson.setCoverImageStorageKey("");
		lesson.setCoverImageOriginalName("");
		lesson.setCoverImageMimeType("");
		lesson.setGenerationMetadata(java.util.Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(List.of());
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}

	private RoadmapLesson roadmapLesson(Roadmap roadmap, Lesson lesson, int sortOrder) {
		RoadmapLesson roadmapLesson = new RoadmapLesson();
		RoadmapLesson.RoadmapLessonId id = new RoadmapLesson.RoadmapLessonId();
		id.setRoadmapId(roadmap.getId());
		id.setLessonId(lesson.getId());
		roadmapLesson.setId(id);
		roadmapLesson.setRoadmap(roadmap);
		roadmapLesson.setLesson(lesson);
		roadmapLesson.setSortOrder(sortOrder);
		roadmapLesson.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return roadmapLesson;
	}

	private UserRoadmap userRoadmap(User user, Roadmap roadmap) {
		UserRoadmap userRoadmap = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setUserId(user.getId());
		id.setRoadmapId(roadmap.getId());
		userRoadmap.setId(id);
		userRoadmap.setUser(user);
		userRoadmap.setRoadmap(roadmap);
		userRoadmap.setEnrolledAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return userRoadmap;
	}

	private UserLesson userLesson(User user, Lesson lesson) {
		UserLesson userLesson = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(user.getId());
		id.setLessonId(lesson.getId());
		userLesson.setId(id);
		userLesson.setUser(user);
		userLesson.setLesson(lesson);
		userLesson.setEnrolledAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return userLesson;
	}
}
