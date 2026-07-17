package com.aidigital.aionboarding.mappers.group;

import com.aidigital.aionboarding.api.v1.model.AddGroupLeadResponseV1;
import com.aidigital.aionboarding.api.v1.model.AddGroupMemberResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupMemberV1;
import com.aidigital.aionboarding.api.v1.model.GroupOrgStatsV1;
import com.aidigital.aionboarding.api.v1.model.GroupResponseV1;
import com.aidigital.aionboarding.api.v1.model.GroupSummaryV1;
import com.aidigital.aionboarding.api.v1.model.GroupV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.group.models.GroupDetailRecord;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupOrgStatsRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupApiMapperImplTest {

	@InjectMocks
	private GroupApiMapperImpl groupApiMapperImpl;

	@Mock
	private UserApiMapper userApiMapper;

	@BeforeEach
	void setUp() {
		when(userApiMapper.toUserSummaryV1(any(UserRecord.class))).thenReturn(Instancio.create(UserSummaryV1.class));
	}

	@Test
	void shouldToGroupSummaryV1GroupSummaryRecordTest() {
		// Given:
		GroupSummaryRecord group = Instancio.create(GroupSummaryRecord.class);

		// When:
		GroupSummaryV1 actualResult = groupApiMapperImpl.toGroupSummaryV1(group);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToGroupSummaryV1GroupSummaryRecordWithNullTest() {
		// Given:
		GroupSummaryRecord group = null;

		// When:
		GroupSummaryV1 actualResult = groupApiMapperImpl.toGroupSummaryV1(group);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToGroupOrgStatsV1GroupOrgStatsRecordTest() {
		// Given:
		GroupOrgStatsRecord stats = Instancio.create(GroupOrgStatsRecord.class);

		// When:
		GroupOrgStatsV1 actualResult = groupApiMapperImpl.toGroupOrgStatsV1(stats);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToGroupOrgStatsV1GroupOrgStatsRecordWithNullTest() {
		// Given:
		GroupOrgStatsRecord stats = null;

		// When:
		GroupOrgStatsV1 actualResult = groupApiMapperImpl.toGroupOrgStatsV1(stats);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToGroupV1GroupDetailRecordTest() {
		// Given:
		GroupDetailRecord group = Instancio.create(GroupDetailRecord.class);

		// When:
		GroupV1 actualResult = groupApiMapperImpl.toGroupV1(group);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToGroupV1GroupDetailRecordWithNullTest() {
		// Given:
		GroupDetailRecord group = null;

		// When:
		GroupV1 actualResult = groupApiMapperImpl.toGroupV1(group);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToGroupMemberV1GroupMemberRecordTest() {
		// Given:
		GroupMemberRecord member = Instancio.create(GroupMemberRecord.class);

		// When:
		GroupMemberV1 actualResult = groupApiMapperImpl.toGroupMemberV1(member);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToGroupMemberV1GroupMemberRecordWithNullTest() {
		// Given:
		GroupMemberRecord member = null;

		// When:
		GroupMemberV1 actualResult = groupApiMapperImpl.toGroupMemberV1(member);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToGroupResponseV1GroupDetailRecordTest() {
		// Given:
		GroupDetailRecord group = Instancio.create(GroupDetailRecord.class);

		// When:
		GroupResponseV1 actualResult = groupApiMapperImpl.toGroupResponseV1(group);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToGroupResponseV1GroupDetailRecordWithNullTest() {
		// Given:
		GroupDetailRecord group = null;

		// When:
		GroupResponseV1 actualResult = groupApiMapperImpl.toGroupResponseV1(group);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToAddGroupMemberResponseV1UserRecordTest() {
		// Given:
		UserRecord member = Instancio.create(UserRecord.class);

		// When:
		AddGroupMemberResponseV1 actualResult = groupApiMapperImpl.toAddGroupMemberResponseV1(member);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAddGroupMemberResponseV1UserRecordWithNullTest() {
		// Given:
		UserRecord member = null;

		// When:
		AddGroupMemberResponseV1 actualResult = groupApiMapperImpl.toAddGroupMemberResponseV1(member);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToAddGroupLeadResponseV1UserRecordTest() {
		// Given:
		UserRecord lead = Instancio.create(UserRecord.class);

		// When:
		AddGroupLeadResponseV1 actualResult = groupApiMapperImpl.toAddGroupLeadResponseV1(lead);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAddGroupLeadResponseV1UserRecordWithNullTest() {
		// Given:
		UserRecord lead = null;

		// When:
		AddGroupLeadResponseV1 actualResult = groupApiMapperImpl.toAddGroupLeadResponseV1(lead);

		// Then:
		assertThat(actualResult).isNull();
	}

}