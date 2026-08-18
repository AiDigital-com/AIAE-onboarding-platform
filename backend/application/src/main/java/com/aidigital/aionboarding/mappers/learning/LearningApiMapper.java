package com.aidigital.aionboarding.mappers.learning;

import com.aidigital.aionboarding.api.v1.model.AssignmentEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.AssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.CompletedRoadmapSummaryV1;
import com.aidigital.aionboarding.api.v1.model.EnrollmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.LearningAssigneeV1;
import com.aidigital.aionboarding.api.v1.model.LearningAssigneesResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityCountsV1;
import com.aidigital.aionboarding.api.v1.model.LessonEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.MyLessonSummaryV1;
import com.aidigital.aionboarding.api.v1.model.MyLessonsResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapAssignmentEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapTeamAssignmentResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapTeamAssignmentV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapTeamAssignmentsListResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonVisibilityApiMapper;
import com.aidigital.aionboarding.mappers.common.PageInfoApiMapper;
import com.aidigital.aionboarding.mappers.lesson.LessonApiMapper;
import com.aidigital.aionboarding.mappers.lessonactivity.LessonActivityApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.learning.models.CompletedRoadmapRecord;
import com.aidigital.aionboarding.service.learning.models.LearningAssigneeRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapAssignmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapEnrollmentResultRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentRecord;
import com.aidigital.aionboarding.service.learning.models.RoadmapTeamAssignmentResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityCountsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(
		config = ApplicationMapperConfig.class,
		uses = {
				UserApiMapper.class,
				LessonApiMapper.class,
				LessonActivityApiMapper.class,
				LessonGenerationStatusApiMapper.class,
				LessonVisibilityApiMapper.class
		}
)
public interface LearningApiMapper extends PageInfoApiMapper {

	LessonEnrollmentV1 toLessonEnrollmentV1(LessonEnrollmentRecord enrollment);

	AssignmentEnrollmentV1 toAssignmentEnrollmentV1(LessonAssignmentEnrollmentRecord enrollment);

	RoadmapAssignmentEnrollmentV1 toRoadmapAssignmentEnrollmentV1(RoadmapAssignmentEnrollmentRecord enrollment);

	RoadmapEnrollmentV1 toRoadmapEnrollmentV1(RoadmapEnrollmentRecord enrollment);

	CompletedRoadmapSummaryV1 toCompletedRoadmapSummaryV1(CompletedRoadmapRecord roadmap);

	LearningAssigneeV1 toLearningAssigneeV1(LearningAssigneeRecord assignee);

	/**
	 * Builds the learning-assignees response. Declared over {@link Page} rather than
	 * {@link java.util.List} directly: MapStruct cannot generate a bean-mapping method whose
	 * sole parameter is a bare {@code java.util} iterable type.
	 *
	 * @param assignees the assignees, wrapped in a page
	 * @return the learning-assignees response
	 */
	@Mapping(target = "assignees", expression = "java(assignees.getContent().stream().map(this::toLearningAssigneeV1).toList())")
	LearningAssigneesResponseV1 toLearningAssigneesResponseV1(Page<LearningAssigneeRecord> assignees);

	/**
	 * Builds the learning-assignees response from a plain list.
	 *
	 * @param assignees the assignees, or {@code null}
	 * @return the learning-assignees response
	 */
	default LearningAssigneesResponseV1 toLearningAssigneesResponseV1(java.util.List<LearningAssigneeRecord> assignees) {
		return toLearningAssigneesResponseV1(
				new org.springframework.data.domain.PageImpl<>(assignees == null ? java.util.List.of() : assignees));
	}

	@Mapping(target = "enrollments", source = "enrollments")
	AssignmentResponseV1 toLessonAssignmentResponseV1(LessonAssignmentResultRecord result);

	@Mapping(target = "enrollments", source = "enrollments")
	AssignmentResponseV1 toRoadmapAssignmentResponseV1(RoadmapAssignmentResultRecord result);

	@Mapping(target = "enrollment", source = "enrollment")
	@Mapping(target = "completedRoadmaps", source = "completedRoadmaps")
	EnrollmentResponseV1 toLessonEnrollmentResponseV1(LessonEnrollmentResultRecord result);

	@Mapping(target = "enrollment", source = "enrollment")
	@Mapping(target = "completedRoadmaps", ignore = true)
	EnrollmentResponseV1 toRoadmapEnrollmentResponseV1(RoadmapEnrollmentResultRecord result);

	LessonActivityCountsV1 toActivityCountsV1(LessonActivityCountsRecord counts);

	@Mapping(target = "enrollment", source = "enrollment")
	@Mapping(target = "activityCounts", source = "activityCounts")
	MyLessonSummaryV1 toMyLessonSummaryV1(MyLessonSummaryRecord record);

	@Mapping(target = "lessons", expression = "java(lessons.getContent().stream().map(this::toMyLessonSummaryV1).toList())")
	@Mapping(target = "page", expression = "java(toPageInfoV1(lessons))")
	MyLessonsResponseV1 toMyLessonsResponseV1(Page<MyLessonSummaryRecord> lessons);

	RoadmapTeamAssignmentV1 toRoadmapTeamAssignmentV1(RoadmapTeamAssignmentRecord assignment);

	@Mapping(target = "assignment", source = "assignment")
	@Mapping(target = "enrollments", source = "enrollments")
	RoadmapTeamAssignmentResponseV1 toRoadmapTeamAssignmentResponseV1(RoadmapTeamAssignmentResultRecord result);

	/**
	 * Builds the roadmap team-assignments list response. Declared over {@link Page} rather than
	 * {@link java.util.List} directly: MapStruct cannot generate a bean-mapping method whose
	 * sole parameter is a bare {@code java.util} iterable type.
	 *
	 * @param assignments the assignments, wrapped in a page
	 * @return the roadmap team-assignments list response
	 */
	@Mapping(target = "assignments", expression = "java(assignments.getContent().stream().map(this::toRoadmapTeamAssignmentV1).toList())")
	RoadmapTeamAssignmentsListResponseV1 toRoadmapTeamAssignmentsListResponseV1(Page<RoadmapTeamAssignmentRecord> assignments);

	/**
	 * Builds the roadmap team-assignments list response from a plain list.
	 *
	 * @param assignments the assignments, or {@code null}
	 * @return the roadmap team-assignments list response
	 */
	default RoadmapTeamAssignmentsListResponseV1 toRoadmapTeamAssignmentsListResponseV1(
			java.util.List<RoadmapTeamAssignmentRecord> assignments
	) {
		return toRoadmapTeamAssignmentsListResponseV1(
				new org.springframework.data.domain.PageImpl<>(assignments == null ? java.util.List.of() : assignments));
	}
}
