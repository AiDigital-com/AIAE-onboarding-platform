package com.aidigital.aionboarding.service.group.support;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.common.time.CurrentTimeImpl;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupRecordAssemblerTest {

	@Mock
	private GroupLeadEntityService groupLeadEntityService;
	@Mock
	private GroupMemberEntityService groupMemberEntityService;
	@Mock
	private UserRecordMapper userMapper;
	@Spy
	private CurrentTime currentTime = new CurrentTimeImpl();

	@InjectMocks
	private GroupRecordAssembler assembler;

	@Test
	void shouldReturnEmptySummaryRecordsForEmptyGroupListTest() {
		// When:
		List<GroupSummaryRecord> result = assembler.toSummaryRecords(List.of());

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void shouldBuildSummaryRecordsWithLeadsAndCountsTest() {
		// Given:
		Group group = new Group();
		group.setId(1L);
		group.setName("Engineering");
		group.setDescription("Engineering group");
		group.setCreatedAt(LocalDateTime.parse("2026-07-15T10:00:00"));
		group.setUpdatedAt(LocalDateTime.parse("2026-07-15T11:00:00"));

		User leadUser = new User();
		leadUser.setId(10L);
		GroupLead groupLead = new GroupLead();
		GroupLead.GroupLeadId id = new GroupLead.GroupLeadId();
		id.setGroupId(1L);
		id.setLeadUserId(10L);
		groupLead.setId(id);
		groupLead.setGroup(group);
		groupLead.setLeadUser(leadUser);

		UserRecord leadRecord = new UserRecord(10L, "clerk-10", "Lead", "lead@test.com", "teamlead", null, null, null,
				null, null, null);
		when(groupLeadEntityService.findByGroupIdIn(eq(List.of(1L)))).thenReturn(List.of(groupLead));
		when(userMapper.toRecord(eq(leadUser))).thenReturn(leadRecord);
		when(groupMemberEntityService.countByGroupIdBatch(eq(List.of(1L)))).thenReturn(Map.of(1L, 5L));
		when(groupMemberEntityService.countMembersWithoutGradeBatch(eq(List.of(1L)))).thenReturn(Map.of(1L, 1L));

		// When:
		List<GroupSummaryRecord> result = assembler.toSummaryRecords(List.of(group));

		// Then:
		assertThat(result).hasSize(1);
		GroupSummaryRecord record = result.get(0);
		assertThat(record.id()).isEqualTo(1L);
		assertThat(record.name()).isEqualTo("Engineering");
		assertThat(record.leads()).containsExactly(leadRecord);
		assertThat(record.membersCount()).isEqualTo(5L);
		assertThat(record.membersWithoutGradeCount()).isEqualTo(1L);
	}

	@Test
	void shouldBuildDetailRecordTest() {
		// Given:
		Group group = new Group();
		group.setId(2L);
		group.setName("Design");
		group.setDescription("Design group");
		group.setCreatedAt(LocalDateTime.parse("2026-07-15T10:00:00"));
		group.setUpdatedAt(LocalDateTime.parse("2026-07-15T11:00:00"));

		User leadUser = new User();
		leadUser.setId(20L);
		GroupLead groupLead = new GroupLead();
		GroupLead.GroupLeadId id = new GroupLead.GroupLeadId();
		id.setGroupId(2L);
		id.setLeadUserId(20L);
		groupLead.setId(id);
		groupLead.setLeadUser(leadUser);

		UserRecord leadRecord = new UserRecord(20L, "clerk-20", "Design Lead", "design@test.com", "teamlead", null,
				null, null, null, null, null);
		when(groupLeadEntityService.findByGroupIdIn(eq(List.of(2L)))).thenReturn(List.of(groupLead));
		when(userMapper.toRecord(eq(leadUser))).thenReturn(leadRecord);

		// When:
		GroupDetailRecord result = assembler.toDetailRecord(group);

		// Then:
		assertThat(result.id()).isEqualTo(2L);
		assertThat(result.leads()).containsExactly(leadRecord);
	}

	@Test
	void shouldMapMemberPageToRecordPageTest() {
		// Given:
		User memberUser = new User();
		memberUser.setId(30L);
		GroupMember member = new GroupMember();
		GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
		id.setGroupId(3L);
		id.setMemberUserId(30L);
		member.setId(id);
		member.setMemberUser(memberUser);
		member.setCreatedAt(LocalDateTime.parse("2026-07-15T10:00:00"));
		Page<GroupMember> page = new PageImpl<>(List.of(member), PageRequest.of(0, 20), 1);
		UserRecord memberRecord = new UserRecord(30L, "clerk-30", "Member", "member@test.com", "member", null, null,
				null, null, null, null);
		when(userMapper.toRecord(eq(memberUser))).thenReturn(memberRecord);

		// When:
		Page<GroupMemberRecord> result = assembler.toMemberRecordPage(page);

		// Then:
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).user()).isEqualTo(memberRecord);
	}

	@Test
	void shouldMapUserToUserRecordTest() {
		// Given:
		User user = new User();
		user.setId(40L);
		UserRecord userRecord = new UserRecord(40L, "clerk-40", "Candidate", "candidate@test.com", "member", null,
				null, null, null, null, null);
		when(userMapper.toRecord(user)).thenReturn(userRecord);

		// When:
		UserRecord result = assembler.toUserRecord(user);

		// Then:
		assertThat(result).isSameAs(userRecord);
	}

	@Nested
	class BuildNewGroup {

		@Test
		void shouldStampNameDescriptionCreatorAndTimestampsTest() {
			// Given:
			User creator = new User();
			creator.setId(50L);

			// When:
			Group result = assembler.buildNewGroup("CS Campaign", "cs campaign", "desc", creator);

			// Then:
			assertThat(result.getName()).isEqualTo("CS Campaign");
			assertThat(result.getNormalizedName()).isEqualTo("cs campaign");
			assertThat(result.getDescription()).isEqualTo("desc");
			assertThat(result.getCreatedByUser()).isSameAs(creator);
			assertThat(result.getCreatedAt()).isNotNull();
			assertThat(result.getUpdatedAt()).isEqualTo(result.getCreatedAt());
		}
	}

	@Nested
	class BuildGroupLead {

		@Test
		void shouldLinkGroupAndLeadWithTimestampTest() {
			// Given:
			Group group = new Group();
			group.setId(60L);
			User leadUser = new User();
			leadUser.setId(70L);

			// When:
			GroupLead result = assembler.buildGroupLead(group, 70L, leadUser);

			// Then:
			assertThat(result.getId().getGroupId()).isEqualTo(60L);
			assertThat(result.getId().getLeadUserId()).isEqualTo(70L);
			assertThat(result.getGroup()).isSameAs(group);
			assertThat(result.getLeadUser()).isSameAs(leadUser);
			assertThat(result.getCreatedAt()).isNotNull();
		}
	}

	@Nested
	class ApplyUpdate {

		@Test
		void shouldMutateNameDescriptionAndUpdatedAtTest() {
			// Given:
			Group group = new Group();
			group.setName("Old Name");
			group.setNormalizedName("old name");
			group.setDescription("old desc");
			group.setUpdatedAt(LocalDateTime.parse("2020-01-01T00:00:00"));

			// When:
			assembler.applyUpdate(group, "New Name", "new name", "new desc");

			// Then:
			assertThat(group.getName()).isEqualTo("New Name");
			assertThat(group.getNormalizedName()).isEqualTo("new name");
			assertThat(group.getDescription()).isEqualTo("new desc");
			assertThat(group.getUpdatedAt()).isAfter(LocalDateTime.parse("2020-01-01T00:00:00"));
		}
	}
}
