package com.aidigital.aionboarding.mappers.team;

import com.aidigital.aionboarding.api.v1.model.AddTeamMemberRequestV1;
import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import com.aidigital.aionboarding.api.v1.model.UserSummaryV1;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link TeamApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class TeamApiMapperTest {

	private final TeamApiMapper mapper = new TeamApiMapperImpl();

	private UserRecord user(String roleCode) {
		return new UserRecord(1L, "clerk-1", "User", "user@test.com", roleCode, "Dev", null, null, null, null, null);
	}

	@Nested
	class ToTeamsResponseV1 {

		@Test
		void shouldReturnNullWhenEveryArgumentIsNullTest() {
			// When:
			var result = mapper.toTeamsResponseV1((Page<TeamRecord>) null, null, null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForEmptyPagesTest() {
			// Given:
			Page<TeamRecord> teams = new PageImpl<>(List.of());
			Page<UserRecord> users = new PageImpl<>(List.of());
			Map<String, Boolean> permissions = Map.of("teams.manage_members", true);

			// When:
			var result = mapper.toTeamsResponseV1(teams, users, permissions);

			// Then:
			assertThat(result.getTeams()).isEmpty();
			assertThat(result.getUsers()).isEmpty();
			assertThat(result.getPermissions()).containsEntry("teams.manage_members", true);
		}

		@Test
		void shouldWrapPlainListsIntoPagesTest() {
			// When:
			var result = mapper.toTeamsResponseV1(List.<TeamRecord>of(), List.<UserRecord>of(), Map.of());

			// Then:
			assertThat(result.getTeams()).isEmpty();
			assertThat(result.getUsers()).isEmpty();
		}

		@Test
		void shouldWrapNullListsIntoEmptyPagesTest() {
			// When:
			var result = mapper.toTeamsResponseV1((List<TeamRecord>) null, (List<UserRecord>) null, Map.of());

			// Then:
			assertThat(result.getTeams()).isEmpty();
			assertThat(result.getUsers()).isEmpty();
		}
	}

	@Nested
	class ResolveMemberRef {

		@Test
		void shouldPreferTheExplicitMemberFieldTest() {
			// Given:
			AddTeamMemberRequestV1 request = new AddTeamMemberRequestV1().member("member-ref").email("e@t.com");

			// When / Then:
			assertThat(mapper.resolveMemberRef(request)).isEqualTo("member-ref");
		}

		@Test
		void shouldFallBackToEmailWhenMemberIsAbsentTest() {
			// Given:
			AddTeamMemberRequestV1 request = new AddTeamMemberRequestV1().email("e@t.com");

			// When / Then:
			assertThat(mapper.resolveMemberRef(request)).isEqualTo("e@t.com");
		}
	}

	@Nested
	class ToUserSummaryForPageAndRoleFallback {

		@Test
		void shouldResolveAKnownRoleTest() {
			// When:
			UserSummaryV1 summary = mapper.toUserSummaryForPage(user("admin"));

			// Then:
			assertThat(summary.getRole()).isEqualTo(UserRoleCodeV1.ADMIN);
		}

		@Test
		void shouldDefaultToMemberForABlankRoleTest() {
			// When:
			UserSummaryV1 summary = mapper.toUserSummaryForPage(user(""));

			// Then:
			assertThat(summary.getRole()).isEqualTo(UserRoleCodeV1.MEMBER);
		}

		@Test
		void shouldDefaultToMemberForANullRoleTest() {
			// When:
			UserSummaryV1 summary = mapper.toUserSummaryForPage(user(null));

			// Then:
			assertThat(summary.getRole()).isEqualTo(UserRoleCodeV1.MEMBER);
		}

		@Test
		void shouldDefaultToMemberForAnUnrecognizedRoleTest() {
			// When:
			UserSummaryV1 summary = mapper.toUserSummaryForPage(user("not-a-role"));

			// Then:
			assertThat(summary.getRole()).isEqualTo(UserRoleCodeV1.MEMBER);
		}
	}
}
