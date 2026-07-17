package com.aidigital.aionboarding.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Measurement harness (not a correctness test): seeds a realistic row count into
 * real PostgreSQL and captures {@code EXPLAIN (ANALYZE, BUFFERS)} for the exact predicate shapes
 * {@link com.aidigital.aionboarding.service.material.support.MaterialSpecificationBuilder},
 * {@link com.aidigital.aionboarding.service.lesson.support.LessonSpecificationBuilder}, and
 * {@link com.aidigital.aionboarding.service.roadmap.support.RoadmapSpecificationBuilder} generate
 * for free-text search, at three term-selectivity levels (absent, rare, common). Plan text is
 * logged at INFO so it is visible in the surefire/console output as evidence for reviewing
 * these query plans; no scan-type assertion is made here.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchQueryPlanEvidenceIntegrationTest {

	private static final Logger LOG = LoggerFactory.getLogger(SearchQueryPlanEvidenceIntegrationTest.class);

	private static final int MATERIAL_COUNT = 12_000;
	private static final int LESSON_COUNT = 8_000;
	private static final int ROADMAP_COUNT = 4_000;
	private static final int BATCH_SIZE = 500;

	private static final String COMMON_TERM = "onboarding";
	private static final String RARE_TERM = "zzzrareterm";
	private static final String ABSENT_TERM = "qqqnonexistentterm999";

	// ~2KB filler, representative of a real material/lesson body, with no quote characters.
	private static final String FILLER_2KB = "the quick brown fox jumps over the lazy dog ".repeat(45);
	private static final String FILLER_200 = "a short paragraph describing the item in a few sentences ".repeat(3);

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeAll
	void seedData() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			seedMaterials();
			seedLessons();
			seedRoadmaps();
		});
	}

	void seedMaterials() {
		StringBuilder values = new StringBuilder();
		for (int i = 0; i < MATERIAL_COUNT; i++) {
			String title = "Material " + i + " sample title" + termSuffix(i);
			appendRow(values, "('" + title + "', '" + FILLER_200 + "', '" + FILLER_2KB + "', '[]'::jsonb, 'seed', now" +
					"(), now())");
			flushBatch(values, i,
					"INSERT INTO materials (title, description, text_content, tags, created_by, created_at, " +
							"updated_at) VALUES ");
		}
	}

	void seedLessons() {
		StringBuilder values = new StringBuilder();
		for (int i = 0; i < LESSON_COUNT; i++) {
			String title = "Lesson " + i + " sample title" + termSuffix(i);
			appendRow(values, "('" + title + "', "
					+ "(SELECT id FROM lesson_status WHERE code = 'ready'), "
					+ "(SELECT id FROM lesson_content_format WHERE code = 'markdown'), "
					+ "(SELECT id FROM lesson_publication_status WHERE code = 'published'), "
					+ "'seed', '[]'::jsonb, now(), now())");
			flushBatch(values, i,
					"INSERT INTO lessons (title, status_id, content_format_id, publication_status_id, created_by, " +
							"tags, created_at, updated_at) VALUES ");
		}
	}

	void seedRoadmaps() {
		StringBuilder values = new StringBuilder();
		for (int i = 0; i < ROADMAP_COUNT; i++) {
			String title = "Roadmap " + i + " sample title" + termSuffix(i);
			appendRow(values, "('" + title + "', '" + FILLER_200 + "', '[]'::jsonb, 'seed', now(), now())");
			flushBatch(values, i, "INSERT INTO roadmaps (title, description, tags, created_by, created_at, updated_at)" +
					" VALUES ");
		}
	}

	/**
	 * Injects COMMON_TERM into ~20% of rows and RARE_TERM into exactly ~1-in-1200 rows.
	 */
	String termSuffix(int i) {
		if (i % 1200 == 0) {
			return " " + RARE_TERM;
		}
		if (i % 5 == 0) {
			return " " + COMMON_TERM;
		}
		return "";
	}

	void appendRow(StringBuilder values, String row) {
		if (!values.isEmpty()) {
			values.append(",");
		}
		values.append(row);
	}

	void flushBatch(StringBuilder values, int index, String insertPrefix) {
		boolean batchFull = (index + 1) % BATCH_SIZE == 0;
		boolean isLastRow =
				(index + 1) == MATERIAL_COUNT || (index + 1) == LESSON_COUNT || (index + 1) == ROADMAP_COUNT;
		if (batchFull || isLastRow) {
			entityManager.createNativeQuery(insertPrefix + values).executeUpdate();
			values.setLength(0);
		}
	}

	List<String> explain(String sql) {
		@SuppressWarnings("unchecked")
		List<Object> rows = entityManager.createNativeQuery("EXPLAIN (ANALYZE, BUFFERS) " + sql).getResultList();
		List<String> lines = rows.stream().map(String::valueOf).toList();
		LOG.info("EXPLAIN evidence for: {}\n{}", sql, String.join("\n", lines));
		return lines;
	}

	/**
	 * Mirrors {@link com.aidigital.aionboarding.service.material.support.MaterialSpecificationBuilder}'s
	 * search predicate. Deliberately excludes {@code text_content} — see that class's comment for
	 * the evidence that justified narrowing it.
	 */
	String materialSearchSelect(String term, boolean withOrder) {
		String pattern = term.toLowerCase();
		String order = withOrder ? " ORDER BY m.created_at DESC LIMIT 24" : "";
		return "SELECT m.id FROM materials m WHERE ("
				+ "lower(m.title) LIKE '%" + pattern + "%'"
				+ " OR lower(m.description) LIKE '%" + pattern + "%'"
				+ " OR lower(m.created_by) LIKE '%" + pattern + "%'"
				+ " OR jsonb_array_contains_ci(m.tags, '" + pattern + "')"
				+ ")" + order;
	}

	String lessonSearchSelect(String term, boolean withOrder) {
		String pattern = term.toLowerCase();
		String order = withOrder ? " ORDER BY l.created_at DESC LIMIT 24" : "";
		return "SELECT l.id FROM lessons l WHERE ("
				+ "lower(l.title) LIKE '%" + pattern + "%'"
				+ " OR jsonb_array_contains_ci(l.tags, '" + pattern + "')"
				+ ")" + order;
	}

	String roadmapSearchSelect(String term, boolean withOrder) {
		String pattern = term.toLowerCase();
		String order = withOrder ? " ORDER BY r.created_at DESC LIMIT 24" : "";
		return "SELECT r.id FROM roadmaps r WHERE ("
				+ "lower(r.title) LIKE '%" + pattern + "%'"
				+ " OR lower(r.description) LIKE '%" + pattern + "%'"
				+ " OR lower(r.created_by) LIKE '%" + pattern + "%'"
				+ " OR jsonb_array_contains_ci(r.tags, '" + pattern + "')"
				+ ")" + order;
	}

	@Test
	void materialsSearchPlanForAnAbsentTermTest() {
		List<String> plan = explain(materialSearchSelect(ABSENT_TERM, true));
		assertThat(plan).isNotEmpty();
	}

	@Test
	void materialsSearchPlanForARareTermTest() {
		List<String> plan = explain(materialSearchSelect(RARE_TERM, true));
		assertThat(plan).isNotEmpty();
	}

	@Test
	void materialsSearchPlanForACommonTermTest() {
		List<String> plan = explain(materialSearchSelect(COMMON_TERM, true));
		assertThat(plan).isNotEmpty();
	}

	@Test
	void materialsCountPlanForACommonTermTest() {
		List<String> plan = explain(materialSearchSelect(COMMON_TERM, false).replace("SELECT m.id", "SELECT count(m" +
				".id)"));
		assertThat(plan).isNotEmpty();
	}

	@Test
	void lessonsSearchPlanForARareTermTest() {
		List<String> plan = explain(lessonSearchSelect(RARE_TERM, true));
		assertThat(plan).isNotEmpty();
	}

	@Test
	void lessonsSearchPlanForACommonTermTest() {
		List<String> plan = explain(lessonSearchSelect(COMMON_TERM, true));
		assertThat(plan).isNotEmpty();
	}

	@Test
	void roadmapsSearchPlanForARareTermTest() {
		List<String> plan = explain(roadmapSearchSelect(RARE_TERM, true));
		assertThat(plan).isNotEmpty();
	}

	@Test
	void roadmapsSearchPlanForACommonTermTest() {
		List<String> plan = explain(roadmapSearchSelect(COMMON_TERM, true));
		assertThat(plan).isNotEmpty();
	}
}
