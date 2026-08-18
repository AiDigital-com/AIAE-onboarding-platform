package com.aidigital.aionboarding.mappers.group;

import com.aidigital.aionboarding.api.v1.model.GroupOrgStatsV1;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.group.models.GroupMemberRecord;
import com.aidigital.aionboarding.service.group.models.GroupOrgStatsRecord;
import com.aidigital.aionboarding.service.group.models.GroupSummaryRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link GroupApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class GroupApiMapperTest {

	private final GroupApiMapper mapper = new GroupApiMapperImpl();

	@Nested
	class ToGroupOrgStatsResponseV1 {

		@Test
		void shouldReturnNullForNullStatsTest() {
			// When:
			var result = mapper.toGroupOrgStatsResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldWrapStatsTest() {
			// Given:
			GroupOrgStatsRecord stats = new GroupOrgStatsRecord(5, 10, 2);

			// When:
			var result = mapper.toGroupOrgStatsResponseV1(stats);

			// Then:
			GroupOrgStatsV1 wrapped = result.getStats();
			assertThat(wrapped.getTotalGroups()).isEqualTo(5);
			assertThat(wrapped.getTotalMembers()).isEqualTo(10);
			assertThat(wrapped.getTotalLeads()).isEqualTo(2);
		}
	}

	@Nested
	class ToGroupsListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toGroupsListResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// Given:
			Page<GroupSummaryRecord> page = new PageImpl<>(List.of());

			// When:
			var result = mapper.toGroupsListResponseV1(page);

			// Then:
			assertThat(result.getGroups()).isEmpty();
		}
	}

	@Nested
	class ToGroupMembersListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toGroupMembersListResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// Given:
			Page<GroupMemberRecord> page = new PageImpl<>(List.of());

			// When:
			var result = mapper.toGroupMembersListResponseV1(page);

			// Then:
			assertThat(result.getMembers()).isEmpty();
		}
	}

	@Nested
	class ToGroupCandidateUsersListResponseV1 {

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageOfCandidatesTest() {
			// Given:
			Page<UserRecord> candidates = new PageImpl<>(List.of());
			UserApiMapper userApiMapper = Mockito.mock(UserApiMapper.class);

			// When:
			var result = mapper.toGroupCandidateUsersListResponseV1(candidates, userApiMapper);

			// Then:
			assertThat(result.getUsers()).isEmpty();
			Mockito.verifyNoInteractions(userApiMapper);
		}
	}
}
