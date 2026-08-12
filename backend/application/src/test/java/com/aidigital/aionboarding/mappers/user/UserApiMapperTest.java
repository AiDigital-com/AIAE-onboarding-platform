package com.aidigital.aionboarding.mappers.user;

import com.aidigital.aionboarding.service.user.models.AdminUserStatsRecord;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link UserApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class UserApiMapperTest {

	private final UserApiMapper mapper = new UserApiMapperImpl();

	@Nested
	class ToAdminUserStatsResponseV1 {

		@Test
		void shouldReturnNullForNullStatsTest() {
			// When:
			var result = mapper.toAdminUserStatsResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldWrapStatsTest() {
			// Given:
			AdminUserStatsRecord stats = new AdminUserStatsRecord(10, 1, 2, 5);

			// When:
			var result = mapper.toAdminUserStatsResponseV1(stats);

			// Then:
			assertThat(result.getStats().getTotalUsers()).isEqualTo(10);
		}
	}

	@Nested
	class ToAdminUsersListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toAdminUsersListResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// When:
			var result = mapper.toAdminUsersListResponseV1(new PageImpl<>(List.<UserRecord>of()));

			// Then:
			assertThat(result.getUsers()).isEmpty();
		}
	}

	@Nested
	class ToUsersListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toUsersListResponseV1((Page<UserRecord>) null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// When:
			var result = mapper.toUsersListResponseV1(new PageImpl<>(List.<UserRecord>of()));

			// Then:
			assertThat(result.getUsers()).isEmpty();
		}

		@Test
		void shouldWrapANullListInAnEmptyPageTest() {
			// When:
			var result = mapper.toUsersListResponseV1((List<UserRecord>) null);

			// Then:
			assertThat(result.getUsers()).isEmpty();
		}

		@Test
		void shouldWrapAPlainListIntoAPageTest() {
			// When:
			var result = mapper.toUsersListResponseV1(List.<UserRecord>of());

			// Then:
			assertThat(result.getUsers()).isEmpty();
		}
	}
}
