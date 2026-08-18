package com.aidigital.aionboarding.service.roadmap.support;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignment;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignmentGrade;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.grade.services.entity.GradeEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentPreviewRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapGroupAssignmentRecord;
import com.aidigital.aionboarding.service.roadmap.services.entity.RoadmapGroupAssignmentEntityService;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapGroupAssignmentRecordAssemblerTest {

	@Mock
	private RoadmapGroupAssignmentEntityService roadmapGroupAssignmentEntityService;
	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private GradeEntityService gradeEntityService;
	@Mock
	private UserRecordMapper userMapper;
	@Mock
	private UserEntityService userEntityService;
	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private RoadmapGroupAssignmentRecordAssembler assembler;

	@Nested
	class CreateAssignment {

		@Test
		void shouldStampTimestampsAndAssignedByUserTest() {
			// Given:
			AppUser actor = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
			Roadmap roadmap = new Roadmap();
			roadmap.setId(10L);
			Group group = new Group();
			group.setId(20L);
			User actorUser = new User();
			when(userEntityService.getReference(1L)).thenReturn(actorUser);
			when(roadmapGroupAssignmentEntityService.save(any(RoadmapGroupAssignment.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			// When:
			RoadmapGroupAssignment result = assembler.createAssignment(actor, roadmap, group);

			// Then:
			assertThat(result.getRoadmap()).isSameAs(roadmap);
			assertThat(result.getGroup()).isSameAs(group);
			assertThat(result.getAssignedByUser()).isSameAs(actorUser);
			assertThat(result.getCreatedAt()).isNotNull();
			assertThat(result.getUpdatedAt()).isEqualTo(result.getCreatedAt());
		}

		@Test
		void shouldAllowNullActorTest() {
			// Given:
			Roadmap roadmap = new Roadmap();
			Group group = new Group();
			when(roadmapGroupAssignmentEntityService.save(any(RoadmapGroupAssignment.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			// When:
			RoadmapGroupAssignment result = assembler.createAssignment(null, roadmap, group);

			// Then:
			assertThat(result.getAssignedByUser()).isNull();
		}
	}

	@Test
	void touchUpdatedAtShouldStampToNowTest() {
		// Given:
		RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
		assignment.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));

		// When:
		assembler.touchUpdatedAt(assignment);

		// Then:
		assertThat(assignment.getUpdatedAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
	}

	@Nested
	class RequireExistingGrades {

		@Test
		void shouldReturnEmptyMapForNullOrEmptyIdsTest() {
			// When-Then:
			assertThat(assembler.requireExistingGrades(null)).isEmpty();
			assertThat(assembler.requireExistingGrades(List.of())).isEmpty();
		}

		@Test
		void shouldThrowWhenAGradeIdDoesNotExistTest() {
			// Given:
			when(gradeEntityService.findById(99L)).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> assembler.requireExistingGrades(List.of(99L)))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldReturnGradesKeyedByIdDeduplicatingTest() {
			// Given:
			Grade grade = new Grade();
			grade.setId(5L);
			when(gradeEntityService.findById(5L)).thenReturn(Optional.of(grade));

			// When:
			Map<Long, Grade> result = assembler.requireExistingGrades(List.of(5L, 5L));

			// Then:
			assertThat(result).containsOnlyKeys(5L);
		}
	}

	@Test
	void toGradeRowsShouldLinkAssignmentAndGradeTest() {
		// Given:
		RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
		assignment.setId(1L);
		Grade grade = new Grade();
		grade.setId(5L);

		// When:
		List<RoadmapGroupAssignmentGrade> result = assembler.toGradeRows(assignment, List.of(grade));

		// Then:
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getId().getAssignmentId()).isEqualTo(1L);
		assertThat(result.get(0).getId().getGradeId()).isEqualTo(5L);
		assertThat(result.get(0).getAssignment()).isSameAs(assignment);
		assertThat(result.get(0).getGrade()).isSameAs(grade);
	}

	@Test
	void gradesOfShouldKeyGradesByIdTest() {
		// Given:
		RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
		assignment.setId(1L);
		Grade grade = new Grade();
		grade.setId(5L);
		RoadmapGroupAssignmentGrade row = new RoadmapGroupAssignmentGrade();
		row.setGrade(grade);
		when(roadmapGroupAssignmentEntityService.findGradesByAssignmentId(1L)).thenReturn(List.of(row));

		// When:
		Map<Long, Grade> result = assembler.gradesOf(assignment);

		// Then:
		assertThat(result).containsEntry(5L, grade);
	}

	@Nested
	class ToRecord {

		@Test
		void shouldAssembleLeadsGradeFiltersAndMemberCountsTest() {
			// Given:
			Roadmap roadmap = new Roadmap();
			roadmap.setId(10L);
			roadmap.setTitle("Onboarding basics");
			Group group = new Group();
			group.setId(20L);
			group.setName("CS Campaign");
			RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
			assignment.setId(99L);
			assignment.setRoadmap(roadmap);
			assignment.setGroup(group);
			assignment.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

			User leadUser = new User();
			leadUser.setId(30L);
			GroupLead groupLead = new GroupLead();
			groupLead.setLeadUser(leadUser);
			UserRecord leadRecord = new UserRecord(30L, "clerk-30", "Lead", "lead@test.com", "teamlead", null, null,
					null, null, null, null);
			when(groupLeadEntityService.findByGroupIdIn(List.of(20L))).thenReturn(List.of(groupLead));
			when(userMapper.toRecord(leadUser)).thenReturn(leadRecord);
			when(groupMemberEntityService.countByGroupId(20L)).thenReturn(3L);
			when(groupMemberEntityService.countMembersWithoutGrade(20L)).thenReturn(1L);

			// When:
			RoadmapGroupAssignmentRecord result = assembler.toRecord(assignment, Map.of());

			// Then:
			assertThat(result.roadmapId()).isEqualTo(10L);
			assertThat(result.roadmapTitle()).isEqualTo("Onboarding basics");
			assertThat(result.groupId()).isEqualTo(20L);
			assertThat(result.groupName()).isEqualTo("CS Campaign");
			assertThat(result.groupLeads()).containsExactly(leadRecord);
			assertThat(result.membersMatchedCount()).isEqualTo(3L);
			assertThat(result.membersWithoutGradeCount()).isEqualTo(1L);
		}

		@Test
		void shouldCountOnlyMembersMatchingGradeFilterTest() {
			// Given:
			Roadmap roadmap = new Roadmap();
			roadmap.setId(10L);
			Group group = new Group();
			group.setId(20L);
			RoadmapGroupAssignment assignment = new RoadmapGroupAssignment();
			assignment.setRoadmap(roadmap);
			assignment.setGroup(group);
			Grade grade = new Grade();
			grade.setId(5L);
			grade.setIsActive(true);
			when(groupLeadEntityService.findByGroupIdIn(List.of(20L))).thenReturn(List.of());
			when(groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(20L, Set.of(5L)))
					.thenReturn(List.of(new GroupMember(), new GroupMember()));
			when(groupMemberEntityService.countMembersWithoutGrade(20L)).thenReturn(0L);

			// When:
			RoadmapGroupAssignmentRecord result = assembler.toRecord(assignment, Map.of(5L, grade));

			// Then:
			assertThat(result.membersMatchedCount()).isEqualTo(2L);
			assertThat(result.gradeFilters()).hasSize(1);
		}
	}

	@Nested
	class PreviewAssignmentTests {

		@Test
		void shouldReturnCountsForGradeFilterTest() {
			// Given:
			Grade grade = new Grade();
			grade.setId(5L);
			when(gradeEntityService.findById(5L)).thenReturn(Optional.of(grade));
			when(groupMemberEntityService.countByGroupId(20L)).thenReturn(10L);
			when(groupMemberEntityService.countMembersWithoutGrade(20L)).thenReturn(2L);
			when(groupMemberEntityService.findByGroupIdAndMemberGradeIdIn(20L, Set.of(5L)))
					.thenReturn(List.of(new GroupMember()));

			// When:
			RoadmapGroupAssignmentPreviewRecord result = assembler.previewAssignment(20L, List.of(5L));

			// Then:
			assertThat(result.groupMembersCount()).isEqualTo(10L);
			assertThat(result.membersMatchedCount()).isEqualTo(1L);
			assertThat(result.membersWithoutGradeCount()).isEqualTo(2L);
		}

		@Test
		void shouldMatchAllMembersWhenNoGradeFilterTest() {
			// Given:
			when(groupMemberEntityService.countByGroupId(20L)).thenReturn(10L);
			when(groupMemberEntityService.countMembersWithoutGrade(20L)).thenReturn(2L);

			// When:
			RoadmapGroupAssignmentPreviewRecord result = assembler.previewAssignment(20L, List.of());

			// Then:
			assertThat(result.membersMatchedCount()).isEqualTo(10L);
		}
	}
}
