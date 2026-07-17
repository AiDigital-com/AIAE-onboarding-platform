package com.aidigital.aionboarding.service.group.services.entity;

import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.group.repositories.GroupMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMemberEntityServiceTest {

	@Mock
	private GroupMemberRepository groupMemberRepository;

	@InjectMocks
	private GroupMemberEntityService service;

	@Test
	void shouldFindByGroupIdTest() {
		// Given:
		GroupMember member = new GroupMember();
		when(groupMemberRepository.findByIdGroupId(1L)).thenReturn(List.of(member));

		// When:
		List<GroupMember> result = service.findByGroupId(1L);

		// Then:
		assertThat(result).containsExactly(member);
	}

	@Test
	void shouldFindByGroupIdWithSearchTest() {
		// Given:
		Page<GroupMember> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
		when(groupMemberRepository.findByIdGroupId(eq(1L), eq("%search%"), eq(PageRequest.of(0, 20))))
				.thenReturn(page);

		// When:
		Page<GroupMember> result = service.findByGroupId(1L, "Search", PageRequest.of(0, 20));

		// Then:
		assertThat(result).isSameAs(page);
	}

	@Test
	void shouldReturnEmptyWhenGroupIdsNullOrEmptyTest() {
		// When-Then:
		assertThat(service.findByGroupIdIn(null)).isEmpty();
		assertThat(service.findByGroupIdIn(List.of())).isEmpty();
	}

	@Test
	void shouldFindByGroupIdInTest() {
		// Given:
		GroupMember member = new GroupMember();
		when(groupMemberRepository.findByIdGroupIdIn(eq(List.of(1L, 2L)))).thenReturn(List.of(member));

		// When:
		List<GroupMember> result = service.findByGroupIdIn(List.of(1L, 2L));

		// Then:
		assertThat(result).containsExactly(member);
	}

	@Test
	void shouldFindAllWithMembersTest() {
		// Given:
		GroupMember member = new GroupMember();
		when(groupMemberRepository.findAllWithMembers()).thenReturn(List.of(member));

		// When:
		List<GroupMember> result = service.findAllWithMembers();

		// Then:
		assertThat(result).containsExactly(member);
	}

	@Test
	void shouldFindGroupIdsByMemberUserIdTest() {
		// Given:
		when(groupMemberRepository.findGroupIdsByIdMemberUserId(5L)).thenReturn(Set.of(1L, 2L));

		// When:
		Set<Long> result = service.findGroupIdsByMemberUserId(5L);

		// Then:
		assertThat(result).containsExactlyInAnyOrder(1L, 2L);
	}

	@Test
	void shouldReturnEmptyWhenGradeIdsNullOrEmptyTest() {
		// When-Then:
		assertThat(service.findByGroupIdAndMemberGradeIdIn(1L, null)).isEmpty();
		assertThat(service.findByGroupIdAndMemberGradeIdIn(1L, List.of())).isEmpty();
	}

	@Test
	void shouldFindByGroupIdAndMemberGradeIdInTest() {
		// Given:
		GroupMember member = new GroupMember();
		when(groupMemberRepository.findByIdGroupIdAndMemberGradeIdIn(eq(1L), eq(List.of(10L, 11L))))
				.thenReturn(List.of(member));

		// When:
		List<GroupMember> result = service.findByGroupIdAndMemberGradeIdIn(1L, List.of(10L, 11L));

		// Then:
		assertThat(result).containsExactly(member);
	}

	@Test
	void shouldCheckExistenceByGroupIdAndMemberUserIdTest() {
		// Given:
		when(groupMemberRepository.existsByIdGroupIdAndIdMemberUserId(1L, 2L)).thenReturn(true);

		// When:
		boolean result = service.existsByGroupIdAndMemberUserId(1L, 2L);

		// Then:
		assertThat(result).isTrue();
	}

	@Test
	void shouldCountByGroupIdTest() {
		// Given:
		when(groupMemberRepository.countByIdGroupId(1L)).thenReturn(5L);

		// When:
		long result = service.countByGroupId(1L);

		// Then:
		assertThat(result).isEqualTo(5L);
	}

	@Test
	void shouldCountMembersWithoutGradeTest() {
		// Given:
		when(groupMemberRepository.countMembersWithoutGrade(1L)).thenReturn(2L);

		// When:
		long result = service.countMembersWithoutGrade(1L);

		// Then:
		assertThat(result).isEqualTo(2L);
	}

	@Test
	void shouldReturnEmptyCountMapWhenGroupIdsNullOrEmptyTest() {
		// When-Then:
		assertThat(service.countByGroupIdBatch(null)).isEmpty();
		assertThat(service.countByGroupIdBatch(List.of())).isEmpty();
	}

	@Test
	void shouldBuildCountMapFromRepositoryRowsTest() {
		// Given:
		when(groupMemberRepository.countByIdGroupIdIn(eq(List.of(1L, 2L))))
				.thenReturn(List.of(new Object[]{1L, 5L}, new Object[]{2L, 3L}));

		// When:
		Map<Long, Long> result = service.countByGroupIdBatch(List.of(1L, 2L));

		// Then:
		assertThat(result).containsEntry(1L, 5L).containsEntry(2L, 3L);
	}

	@Test
	void shouldCountDistinctMemberUsersTest() {
		// Given:
		when(groupMemberRepository.countDistinctMemberUsers()).thenReturn(12L);

		// When:
		long result = service.countDistinctMemberUsers();

		// Then:
		assertThat(result).isEqualTo(12L);
	}

	@Test
	void shouldReturnZeroDistinctMemberUsersWhenGroupIdsEmptyTest() {
		// When-Then:
		assertThat(service.countDistinctMemberUsers(List.of())).isZero();
	}

	@Test
	void shouldCountDistinctMemberUsersByGroupIdsTest() {
		// Given:
		when(groupMemberRepository.countDistinctMemberUsersByIdGroupIdIn(eq(List.of(1L, 2L))))
				.thenReturn(8L);

		// When:
		long result = service.countDistinctMemberUsers(List.of(1L, 2L));

		// Then:
		assertThat(result).isEqualTo(8L);
	}

	@Test
	void shouldSaveGroupMemberTest() {
		// Given:
		GroupMember member = new GroupMember();
		when(groupMemberRepository.save(member)).thenReturn(member);

		// When:
		GroupMember result = service.save(member);

		// Then:
		assertThat(result).isSameAs(member);
	}

	@Test
	void shouldDeleteByIdTest() {
		// Given:
		GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
		id.setGroupId(1L);
		id.setMemberUserId(2L);

		// When:
		service.deleteById(id);

		// Then:
		verify(groupMemberRepository).deleteById(id);
	}

	@Test
	void shouldCheckExistsByIdTest() {
		// Given:
		GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
		id.setGroupId(1L);
		id.setMemberUserId(2L);
		when(groupMemberRepository.existsById(id)).thenReturn(true);

		// When:
		boolean result = service.existsById(id);

		// Then:
		assertThat(result).isTrue();
	}
}
