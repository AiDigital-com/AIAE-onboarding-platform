package com.aidigital.aionboarding.teamdashboard;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonContentFormatCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.ActivityTypeRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.repositories.LessonRepository;
import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.LessonActivityRepository;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityAttemptRepository;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.LowConfidenceLessonProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.TeamDashboardRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link TeamDashboardRepository#findLowConfidenceLessons}
 * against real PostgreSQL: {@code attempts}/{@code learners}/{@code avgScore} (and their
 * {@code *ExcludingLead} counterparts) must reflect the FULL attempt history even when it exceeds
 * the embedded sample cap, while {@code attemptItems} itself must be bounded to the cap and
 * ordered most-recent-first — KPIs must never be computed from the capped sample.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TeamDashboardLowConfidenceLessonsRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	private static final int SAMPLE_SIZE = 5;

	@Autowired
	private TeamDashboardRepository teamDashboardRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonStatusRepository lessonStatusRepository;

	@Autowired
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;

	@Autowired
	private LessonContentFormatRepository lessonContentFormatRepository;

	@Autowired
	private LessonActivityRepository lessonActivityRepository;

	@Autowired
	private ActivityTypeRepository activityTypeRepository;

	@Autowired
	private UserLessonActivityAttemptRepository attemptRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void findLowConfidenceLessonsShouldAggregateOverFullHistoryWhileCappingAndOrderingAttemptItemsTest() throws Exception {
		// Given: a lead with 2 recent high-scoring attempts and a member with 5 older
		// lower-scoring attempts on the same lesson — 7 attempts total, more than the cap of 5
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User lead = userRepository.save(user(role, "Lead", "lead@test.com"));
		User member = userRepository.save(user(role, "Member", "member@test.com"));
		Lesson lesson = lessonRepository.save(lesson("Struggling Lesson"));
		ActivityType quiz = activityTypeRepository.findByCode(ActivityTypeCode.QUIZ).orElseThrow();
		LessonActivity activity = lessonActivityRepository.save(activity(lesson, quiz));

		LocalDateTime day1 = LocalDateTime.of(2026, 1, 1, 0, 0);
		attemptRepository.save(attempt(member, lesson, activity, 1, 40, day1));
		attemptRepository.save(attempt(member, lesson, activity, 2, 45, day1.plusDays(1)));
		attemptRepository.save(attempt(member, lesson, activity, 3, 50, day1.plusDays(2)));
		attemptRepository.save(attempt(member, lesson, activity, 4, 55, day1.plusDays(3)));
		attemptRepository.save(attempt(member, lesson, activity, 5, 60, day1.plusDays(4)));
		attemptRepository.save(attempt(lead, lesson, activity, 1, 90, day1.plusDays(5)));
		attemptRepository.save(attempt(lead, lesson, activity, 2, 95, day1.plusDays(6)));

		// When:
		List<LowConfidenceLessonProjection> result =
				teamDashboardRepository.findLowConfidenceLessons(List.of(lead.getId(), member.getId()), lead.getId(),
						SAMPLE_SIZE);

		// Then: KPIs reflect all 7 attempts, not just the capped sample
		assertThat(result).hasSize(1);
		LowConfidenceLessonProjection row = result.get(0);
		assertThat(row.getAttempts()).isEqualTo(7);
		assertThat(row.getLearners()).isEqualTo(2);
		assertThat(row.getAvgScore()).isEqualTo(62); // round((40+45+50+55+60+90+95)/7) = round(62.142) = 62

		// And: excluding-lead KPIs are computed from the full member-only history (5 attempts)
		assertThat(row.getAttemptsExcludingLead()).isEqualTo(5);
		assertThat(row.getLearnersExcludingLead()).isEqualTo(1);
		assertThat(row.getAvgScoreExcludingLead()).isEqualTo(50); // (40+45+50+55+60)/5 = 50

		// And: attemptItems is capped to the sample size and ordered most-recent-first, so it
		// drops the member's two oldest attempts (day1, day1+1) while keeping everything newer
		List<Map<String, Object>> attemptItems = parseAttemptItems(row.getAttemptItems());
		assertThat(attemptItems).hasSize(SAMPLE_SIZE);
		assertThat(attemptItems).extracting(item -> ((Number) item.get("score")).intValue())
				.containsExactly(95, 90, 60, 55, 50);
	}

	@Test
	void findLowConfidenceLessonsShouldTreatExcludingLeadFieldsAsFullAggregatesWhenLeadIdIsNullTest() {
		// Given: an admin/org-wide scope has no lead concept (leadId null)
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User memberOne = userRepository.save(user(role, "One", "one@test.com"));
		User memberTwo = userRepository.save(user(role, "Two", "two@test.com"));
		Lesson lesson = lessonRepository.save(lesson("Org Lesson"));
		ActivityType quiz = activityTypeRepository.findByCode(ActivityTypeCode.QUIZ).orElseThrow();
		LessonActivity activity = lessonActivityRepository.save(activity(lesson, quiz));
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		attemptRepository.save(attempt(memberOne, lesson, activity, 1, 30, now));
		attemptRepository.save(attempt(memberTwo, lesson, activity, 1, 50, now.plusDays(1)));

		// When: no lead in scope
		List<LowConfidenceLessonProjection> result = teamDashboardRepository.findLowConfidenceLessons(
				List.of(memberOne.getId(), memberTwo.getId()), null, SAMPLE_SIZE);

		// Then: "excluding lead" fields equal the full aggregates — IS DISTINCT FROM NULL matches
		// every row instead of a plain <> comparison silently excluding everything
		assertThat(result).hasSize(1);
		LowConfidenceLessonProjection row = result.get(0);
		assertThat(row.getAttemptsExcludingLead()).isEqualTo(row.getAttempts());
		assertThat(row.getLearnersExcludingLead()).isEqualTo(row.getLearners());
		assertThat(row.getAvgScoreExcludingLead()).isEqualTo(row.getAvgScore());
	}

	private List<Map<String, Object>> parseAttemptItems(String json) throws Exception {
		return objectMapper.readValue(json, new TypeReference<>() {
		});
	}

	private User user(UserRole role, String name, String email) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
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
		lesson.setGenerationMetadata(Map.of());
		lesson.setRevisionHistory(List.of());
		lesson.setErrorMessage("");
		lesson.setPublicationStatus(publicationStatus);
		lesson.setCreatedBy("tester");
		lesson.setTags(List.of());
		lesson.setCreatedAt(now);
		lesson.setUpdatedAt(now);
		return lesson;
	}

	private LessonActivity activity(Lesson lesson, ActivityType type) {
		LessonActivity activity = new LessonActivity();
		activity.setLesson(lesson);
		activity.setType(type);
		activity.setTitle("Quiz");
		activity.setItemCount(1);
		activity.setPayload(Map.of());
		activity.setGenerationMetadata(Map.of());
		activity.setCreatedBy("tester");
		activity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return activity;
	}

	private UserLessonActivityAttempt attempt(
			User user, Lesson lesson, LessonActivity activity, int attemptNumber, int score, LocalDateTime createdAt
	) {
		UserLessonActivityAttempt attempt = new UserLessonActivityAttempt();
		attempt.setUser(user);
		attempt.setLesson(lesson);
		attempt.setActivity(activity);
		attempt.setType(activity.getType());
		attempt.setAttemptNumber(attemptNumber);
		attempt.setScore(BigDecimal.valueOf(score));
		attempt.setPassed(score >= 80);
		attempt.setCorrectCount(score / 10);
		attempt.setTotalCount(10);
		attempt.setSubmittedAnswers(List.of());
		attempt.setResults(List.of());
		attempt.setMetadata(Map.of());
		attempt.setCreatedAt(createdAt);
		return attempt;
	}
}
