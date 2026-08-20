package com.aidigital.aionboarding.teamdashboard;

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
import com.aidigital.aionboarding.domain.teamdashboard.repositories.MemberStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.RoadmapStatsProjection;
import com.aidigital.aionboarding.domain.teamdashboard.repositories.TeamDashboardRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link TeamDashboardRepository#findRoadmapStats} and
 * {@link TeamDashboardRepository#findMemberStats} against real PostgreSQL: a roadmap's lesson
 * count must reflect only <b>learnable</b> lessons ({@code ready} and either {@code published}
 * or {@code private}), the same set {@code RoadmapEnrollmentServiceImpl.fanOutRoadmapLessons}
 * grants {@code user_lessons} rows for. An archived (or still-generating) roadmap lesson never
 * receives a {@code user_lessons} row, so counting it in the denominator would leave a member's
 * roadmap progress permanently short of 100% even once every lesson they were actually given is
 * complete — the exact same asymmetry fixed in
 * {@code UserRoadmapRepositoryImpl#findCompletedRoadmapsForUserLesson}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TeamDashboardRoadmapProgressRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

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
	private RoadmapRepository roadmapRepository;

	@Autowired
	private RoadmapLessonRepository roadmapLessonRepository;

	@Autowired
	private UserRoadmapRepository userRoadmapRepository;

	@Autowired
	private UserLessonRepository userLessonRepository;

	@Test
	void findRoadmapStatsShouldReportFullProgressWhenOnlyTheArchivedLessonIsIncompleteTest() {
		// Given: a roadmap with one published (completed) lesson and one archived lesson that was
		// never fanned out, so it has no user_lessons row at all
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User member = userRepository.save(user(role, "Member", "roadmap-progress-member@test.com"));
		Lesson publishedLesson = lessonRepository.save(lesson("Published", publicationStatus(LessonPublicationStatusCode.PUBLISHED)));
		Lesson archivedLesson = lessonRepository.save(lesson("Archived", publicationStatus(LessonPublicationStatusCode.ARCHIVED)));
		Roadmap roadmap = roadmapRepository.save(roadmap("Progress Roadmap"));
		roadmapLessonRepository.save(roadmapLesson(roadmap, publishedLesson, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmap, archivedLesson, 1));
		userRoadmapRepository.save(userRoadmap(member, roadmap));
		userLessonRepository.save(completedUserLesson(member, publishedLesson));

		// When:
		List<RoadmapStatsProjection> result = teamDashboardRepository.findRoadmapStats(
				List.of(member.getId()), LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED,
				LessonPublicationStatusCode.PRIVATE
		);

		// Then: lessonCount only counts the learnable lesson, so progress reads 100%, not 50%
		assertThat(result).hasSize(1);
		RoadmapStatsProjection row = result.get(0);
		assertThat(row.getLessonCount()).isEqualTo(1);
		assertThat(row.getAvgProgress()).isEqualTo(100);
	}

	@Test
	void findMemberStatsShouldCountOnlyLearnableRoadmapLessonsTest() {
		// Given: same roadmap shape as above, exercised through the per-member projection instead
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User member = userRepository.save(user(role, "Member2", "member-stats-member@test.com"));
		Lesson publishedLesson = lessonRepository.save(lesson("Published2", publicationStatus(LessonPublicationStatusCode.PUBLISHED)));
		Lesson archivedLesson = lessonRepository.save(lesson("Archived2", publicationStatus(LessonPublicationStatusCode.ARCHIVED)));
		Roadmap roadmap = roadmapRepository.save(roadmap("Member Progress Roadmap"));
		roadmapLessonRepository.save(roadmapLesson(roadmap, publishedLesson, 0));
		roadmapLessonRepository.save(roadmapLesson(roadmap, archivedLesson, 1));
		userRoadmapRepository.save(userRoadmap(member, roadmap));
		userLessonRepository.save(completedUserLesson(member, publishedLesson));

		// When:
		List<MemberStatsProjection> result = teamDashboardRepository.findMemberStats(
				List.of(member.getId()), LessonStatusCode.READY, LessonPublicationStatusCode.PUBLISHED,
				LessonPublicationStatusCode.PRIVATE
		);

		// Then: roadmapLessonCount/roadmapCompletedCount both reflect only the learnable lesson
		assertThat(result).hasSize(1);
		MemberStatsProjection row = result.get(0);
		assertThat(row.getRoadmapLessonCount()).isEqualTo(1);
		assertThat(row.getRoadmapCompletedCount()).isEqualTo(1);
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

	private LessonPublicationStatus publicationStatus(String code) {
		return lessonPublicationStatusRepository.findByCode(code).orElseThrow();
	}

	private Lesson lesson(String title, LessonPublicationStatus publicationStatus) {
		LessonStatus status = lessonStatusRepository.findByCode(LessonStatusCode.READY).orElseThrow();
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

	private UserLesson completedUserLesson(User user, Lesson lesson) {
		UserLesson userLesson = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(user.getId());
		id.setLessonId(lesson.getId());
		userLesson.setId(id);
		userLesson.setUser(user);
		userLesson.setLesson(lesson);
		userLesson.setEnrolledAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		userLesson.setCompletedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
		return userLesson;
	}
}
