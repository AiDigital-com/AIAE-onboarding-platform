package com.aidigital.aionboarding.service.learning.support;

import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapTeamAssignment;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearningEnrollmentSupportTest {

	private final LearningEnrollmentSupport support = new LearningEnrollmentSupport();

	@Test
	void shouldBuildUserLessonIdTest() {
		// When:
		UserLesson.UserLessonId result = support.userLessonId(1L, 2L);

		// Then:
		assertThat(result.getUserId()).isEqualTo(1L);
		assertThat(result.getLessonId()).isEqualTo(2L);
	}

	@Test
	void shouldBuildUserRoadmapIdTest() {
		// When:
		UserRoadmap.UserRoadmapId result = support.userRoadmapId(3L, 4L);

		// Then:
		assertThat(result.getUserId()).isEqualTo(3L);
		assertThat(result.getRoadmapId()).isEqualTo(4L);
	}

	@Test
	void shouldNormalizeUserIdsDroppingNullsAndDuplicatesTest() {
		// Given:
		List<Long> userIds = java.util.Arrays.asList(1L, null, 2L, 1L, 3L);

		// When:
		List<Long> result = support.normalizeUserIds(userIds);

		// Then:
		assertThat(result).containsExactly(1L, 2L, 3L);
	}

	@Test
	void shouldReturnEmptyNormalizedUserIdsForNullOrEmptyInputTest() {
		// When-Then:
		assertThat(support.normalizeUserIds(null)).isEmpty();
		assertThat(support.normalizeUserIds(List.of())).isEmpty();
	}

	@Test
	void shouldMapLessonEnrollmentWhenNotCompletedTest() {
		// Given:
		UserLesson enrollment = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setLessonId(10L);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.parse("2026-07-15T10:00:00"));
		enrollment.setCompletedAt(null);

		// When:
		LessonEnrollmentRecord result = support.toLessonEnrollment(enrollment);

		// Then:
		assertThat(result.lessonId()).isEqualTo(10L);
		assertThat(result.enrolledAt()).isEqualTo("2026-07-15T10:00:00");
		assertThat(result.completedAt()).isNull();
		assertThat(result.isCompleted()).isFalse();
	}

	@Test
	void shouldMapLessonEnrollmentWhenCompletedTest() {
		// Given:
		UserLesson enrollment = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setLessonId(11L);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.parse("2026-07-15T10:00:00"));
		enrollment.setCompletedAt(LocalDateTime.parse("2026-07-16T10:00:00"));

		// When:
		LessonEnrollmentRecord result = support.toLessonEnrollment(enrollment);

		// Then:
		assertThat(result.isCompleted()).isTrue();
		assertThat(result.completedAt()).isEqualTo("2026-07-16T10:00:00");
	}

	@Test
	void shouldMapLessonAssignmentEnrollmentTest() {
		// Given:
		UserLesson enrollment = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setLessonId(12L);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.parse("2026-07-15T10:00:00"));
		enrollment.setCompletedAt(null);

		// When:
		LessonAssignmentEnrollmentRecord result = support.toLessonAssignmentEnrollment(enrollment, 7L);

		// Then:
		assertThat(result.userId()).isEqualTo(7L);
		assertThat(result.lessonId()).isEqualTo(12L);
		assertThat(result.isCompleted()).isFalse();
	}

	@Test
	void shouldMapRoadmapEnrollmentTest() {
		// Given:
		UserRoadmap enrollment = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setRoadmapId(20L);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.parse("2026-07-15T11:00:00"));

		// When:
		RoadmapEnrollmentRecord result = support.toRoadmapEnrollment(enrollment);

		// Then:
		assertThat(result.roadmapId()).isEqualTo(20L);
		assertThat(result.enrolledAt()).isEqualTo("2026-07-15T11:00:00");
	}

	@Test
	void shouldMapRoadmapAssignmentEnrollmentTest() {
		// Given:
		UserRoadmap enrollment = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setRoadmapId(21L);
		enrollment.setId(id);
		enrollment.setEnrolledAt(LocalDateTime.parse("2026-07-15T12:00:00"));

		// When:
		RoadmapAssignmentEnrollmentRecord result = support.toRoadmapAssignmentEnrollment(enrollment, 8L);

		// Then:
		assertThat(result.userId()).isEqualTo(8L);
		assertThat(result.roadmapId()).isEqualTo(21L);
	}

	@Test
	void shouldMapRoadmapTeamAssignmentWithAssignedByUserTest() {
		// Given:
		Roadmap roadmap = new Roadmap();
		roadmap.setId(30L);
		User lead = new User();
		lead.setId(40L);
		User assignedBy = new User();
		assignedBy.setId(50L);
		RoadmapTeamAssignment assignment = new RoadmapTeamAssignment();
		assignment.setId(1L);
		assignment.setRoadmap(roadmap);
		assignment.setLeadUser(lead);
		assignment.setAssignedByUser(assignedBy);
		assignment.setCreatedAt(LocalDateTime.parse("2026-07-15T13:00:00"));

		// When:
		RoadmapTeamAssignmentRecord result = support.toRoadmapTeamAssignment(assignment);

		// Then:
		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.roadmapId()).isEqualTo(30L);
		assertThat(result.leadUserId()).isEqualTo(40L);
		assertThat(result.assignedByUserId()).isEqualTo(50L);
		assertThat(result.createdAt()).isEqualTo("2026-07-15T13:00:00");
	}

	@Test
	void shouldMapRoadmapTeamAssignmentWithNullAssignedByUserTest() {
		// Given:
		Roadmap roadmap = new Roadmap();
		roadmap.setId(31L);
		User lead = new User();
		lead.setId(41L);
		RoadmapTeamAssignment assignment = new RoadmapTeamAssignment();
		assignment.setId(2L);
		assignment.setRoadmap(roadmap);
		assignment.setLeadUser(lead);
		assignment.setAssignedByUser(null);
		assignment.setCreatedAt(LocalDateTime.parse("2026-07-15T14:00:00"));

		// When:
		RoadmapTeamAssignmentRecord result = support.toRoadmapTeamAssignment(assignment);

		// Then:
		assertThat(result.assignedByUserId()).isNull();
	}
}
