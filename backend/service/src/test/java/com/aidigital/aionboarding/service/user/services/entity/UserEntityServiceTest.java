package com.aidigital.aionboarding.service.user.services.entity;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEntityServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserEntityService service;

	@Test
	void shouldFindByClerkUserIdTest() {
		// Given:
		User user = new User();
		when(userRepository.findByClerkUserId("clerk-1")).thenReturn(Optional.of(user));

		// When:
		Optional<User> result = service.findByClerkUserId("clerk-1");

		// Then:
		assertThat(result).contains(user);
	}

	@Test
	void shouldFindByEmailTest() {
		// Given:
		User user = new User();
		when(userRepository.findByEmail("email@test.com")).thenReturn(Optional.of(user));

		// When:
		Optional<User> result = service.findByEmail("email@test.com");

		// Then:
		assertThat(result).contains(user);
	}

	@Test
	void shouldFindByIdTest() {
		// Given:
		User user = new User();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		// When:
		Optional<User> result = service.findById(1L);

		// Then:
		assertThat(result).contains(user);
	}

	@Test
	void shouldFindByIdForUpdateTest() {
		// Given:
		User user = new User();
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

		// When:
		Optional<User> result = service.findByIdForUpdate(1L);

		// Then:
		assertThat(result).contains(user);
	}

	@Test
	void shouldFindAllTest() {
		// Given:
		User user = new User();
		when(userRepository.findAll()).thenReturn(List.of(user));

		// When:
		List<User> result = service.findAll();

		// Then:
		assertThat(result).containsExactly(user);
	}

	@Test
	void shouldReturnEmptyWhenRoleCodesNullOrEmptyTest() {
		// When-Then:
		assertThat(service.findByRoleCodeIn(null)).isEmpty();
		assertThat(service.findByRoleCodeIn(List.of())).isEmpty();
	}

	@Test
	void shouldFindByRoleCodeInTest() {
		// Given:
		User user = new User();
		when(userRepository.findByRoleCodeIn(eq(List.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD))))
				.thenReturn(List.of(user));

		// When:
		List<User> result = service.findByRoleCodeIn(List.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD));

		// Then:
		assertThat(result).containsExactly(user);
	}

	@Test
	void shouldReturnEmptyPageWhenRoleCodesEmptyTest() {
		// Given:
		Pageable pageable = PageRequest.of(0, 20);

		// When:
		Page<User> result = service.findByRoleCodeIn(List.of(), "query", pageable);

		// Then:
		assertThat(result.getContent()).isEmpty();
	}

	@Test
	void shouldFindByRoleCodeInWithQueryTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findByRoleCodeIn(eq(List.of(UserRoleCode.MEMBER)), eq("Query"), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findByRoleCodeIn(List.of(UserRoleCode.MEMBER), "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldFindByRoleCodeInWithoutQueryTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findByRoleCodeIn(eq(List.of(UserRoleCode.MEMBER)), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findByRoleCodeIn(List.of(UserRoleCode.MEMBER), "  ", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldFindAllExcludingTest() {
		// Given:
		User user = new User();
		when(userRepository.findAllExcluding(1L)).thenReturn(List.of(user));

		// When:
		List<User> result = service.findAllExcluding(1L);

		// Then:
		assertThat(result).containsExactly(user);
	}

	@Test
	void shouldFindAllExcludingWithQueryTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findAllExcluding(eq(1L), eq("Query"), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findAllExcluding(1L, "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldFindAllExcludingWithoutQueryTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findAllExcluding(eq(1L), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findAllExcluding(1L, "", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldFindTeamCandidatesTest() {
		// Given:
		User user = new User();
		when(userRepository.findTeamCandidates(1L, UserRoleCode.ADMIN)).thenReturn(List.of(user));

		// When:
		List<User> result = service.findTeamCandidates(1L, UserRoleCode.ADMIN);

		// Then:
		assertThat(result).containsExactly(user);
	}

	@Test
	void shouldFindTeamCandidatesWithQueryTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findTeamCandidates(eq(1L), eq(UserRoleCode.ADMIN), eq("Query"), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findTeamCandidates(1L, UserRoleCode.ADMIN, "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldFindTeamCandidatesWithoutQueryTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findTeamCandidates(eq(1L), eq(UserRoleCode.ADMIN), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findTeamCandidates(1L, UserRoleCode.ADMIN, "  ", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldFindGroupMemberCandidatesTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findGroupMemberCandidates(eq(Set.of(2L, 3L)), eq("%query%"), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findGroupMemberCandidates(Set.of(2L, 3L), "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldUseNegativeSentinelForEmptyExcludeIdsTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findGroupMemberCandidates(eq(Set.of(-1L)), eq("%query%"), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findGroupMemberCandidates(null, "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldFindGroupLeadCandidatesTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findGroupLeadCandidates(
				eq(Set.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD)),
				eq(Set.of(2L)),
				eq("%query%"),
				eq(pageable)
		)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findGroupLeadCandidates(Set.of(2L), "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldSearchUsersTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.search(eq(UserRoleCode.MEMBER), eq("%query%"), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.search(UserRoleCode.MEMBER, "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldReturnEmptyPageWhenGroupIdsEmptyTest() {
		// Given:
		Pageable pageable = PageRequest.of(0, 20);

		// When:
		Page<User> result = service.findGroupMemberUsersByGroupIdIn(List.of(), 1L, "query", pageable);

		// Then:
		assertThat(result.getContent()).isEmpty();
	}

	@Test
	void shouldFindGroupMemberUsersByGroupIdInTest() {
		// Given:
		User user = new User();
		Pageable pageable = PageRequest.of(0, 20);
		when(userRepository.findGroupMemberUsersByGroupIdIn(eq(List.of(1L, 2L)), eq(3L), eq("%query%"), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(user), pageable, 1));

		// When:
		Page<User> result = service.findGroupMemberUsersByGroupIdIn(List.of(1L, 2L), 3L, "Query", pageable);

		// Then:
		assertThat(result.getContent()).containsExactly(user);
	}

	@Test
	void shouldCountTest() {
		// Given:
		when(userRepository.count()).thenReturn(10L);

		// When:
		long result = service.count();

		// Then:
		assertThat(result).isEqualTo(10L);
	}

	@Test
	void shouldCountByRoleCodeTest() {
		// Given:
		when(userRepository.countByRole_Code(UserRoleCode.ADMIN)).thenReturn(2L);

		// When:
		long result = service.countByRoleCode(UserRoleCode.ADMIN);

		// Then:
		assertThat(result).isEqualTo(2L);
	}

	@Test
	void shouldFindByNameIgnoreCaseTest() {
		// Given:
		User user = new User();
		when(userRepository.findByNameIgnoreCase("Name")).thenReturn(Optional.of(user));

		// When:
		Optional<User> result = service.findByNameIgnoreCase("Name");

		// Then:
		assertThat(result).contains(user);
	}

	@Test
	void shouldGetReferenceTest() {
		// Given:
		User user = new User();
		when(userRepository.getReferenceById(1L)).thenReturn(user);

		// When:
		User result = service.getReference(1L);

		// Then:
		assertThat(result).isSameAs(user);
	}

	@Test
	void shouldSaveUserTest() {
		// Given:
		User user = new User();
		when(userRepository.save(user)).thenReturn(user);

		// When:
		User result = service.save(user);

		// Then:
		assertThat(result).isSameAs(user);
	}

	@Test
	void shouldNormalizeSearchTest() {
		// When-Then:
		assertThat(service.normalizeSearch("  query  ")).isEqualTo("query");
		assertThat(service.normalizeSearch(null)).isNull();
		assertThat(service.normalizeSearch("   ")).isNull();
	}

	@Test
	void shouldBuildLikeSearchTest() {
		// When:
		String result = service.likeSearch("Query");

		// Then:
		assertThat(result).isEqualTo("%query%");
	}

	@Test
	void shouldReturnNullLikeSearchForBlankInputTest() {
		// When-Then:
		assertThat(service.likeSearch(null)).isNull();
		assertThat(service.likeSearch("   ")).isNull();
	}
}
