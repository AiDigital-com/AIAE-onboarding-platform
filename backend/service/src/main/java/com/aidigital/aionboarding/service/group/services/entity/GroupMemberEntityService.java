package com.aidigital.aionboarding.service.group.services.entity;

import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.group.repositories.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Short-transaction CRUD helpers for the {@link GroupMember} entity.
 * <p>
 * This is the only service that may inject {@link GroupMemberRepository} directly. All other
 * services that require group-membership data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class GroupMemberEntityService {

    private final GroupMemberRepository groupMemberRepository;

    /**
     * Loads every member of a group, with member users and their grade eagerly fetched.
     *
     * @param groupId group primary key
     * @return matching memberships
     */
    @Transactional(readOnly = true)
    public List<GroupMember> findByGroupId(Long groupId) {
        return groupMemberRepository.findByIdGroupId(groupId);
    }

    /**
     * Loads one page of a group's members, optionally filtered by a case-insensitive name/email
     * search, with member users and their grade eagerly fetched.
     *
     * @param groupId  group primary key
     * @param search   optional case-insensitive name/email search
     * @param pageable page and sort request
     * @return matching memberships page
     */
    @Transactional(readOnly = true)
    public Page<GroupMember> findByGroupId(Long groupId, String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank())
            ? null
            : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return groupMemberRepository.findByIdGroupId(groupId, normalizedSearch, pageable);
    }

    /**
     * Loads every member across several groups at once, with member users and their grade
     * eagerly fetched, so a lead who leads multiple groups can be resolved in one query.
     *
     * @param groupIds group primary keys
     * @return matching memberships across all given groups
     */
    @Transactional(readOnly = true)
    public List<GroupMember> findByGroupIdIn(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        return groupMemberRepository.findByIdGroupIdIn(groupIds);
    }

    /**
     * Loads every group membership in the workspace, with member users and their grade eagerly
     * fetched. Used for admin-scoped views that need the full membership set.
     *
     * @return every membership
     */
    @Transactional(readOnly = true)
    public List<GroupMember> findAllWithMembers() {
        return groupMemberRepository.findAllWithMembers();
    }

    /**
     * Loads the ids of every group the given user belongs to.
     *
     * @param memberUserId member user primary key
     * @return matching group ids
     */
    @Transactional(readOnly = true)
    public Set<Long> findGroupIdsByMemberUserId(Long memberUserId) {
        return groupMemberRepository.findGroupIdsByIdMemberUserId(memberUserId);
    }

    /**
     * Loads the members of a group whose current grade id is one of {@code gradeIds}.
     *
     * @param groupId  group primary key
     * @param gradeIds candidate grade ids
     * @return matching memberships
     */
    @Transactional(readOnly = true)
    public List<GroupMember> findByGroupIdAndMemberGradeIdIn(Long groupId, Collection<Long> gradeIds) {
        if (gradeIds == null || gradeIds.isEmpty()) {
            return List.of();
        }
        return groupMemberRepository.findByIdGroupIdAndMemberGradeIdIn(groupId, gradeIds);
    }

    /**
     * Checks whether a user belongs to a specific group.
     *
     * @param groupId      group primary key
     * @param memberUserId member user primary key
     * @return {@code true} when the membership exists
     */
    @Transactional(readOnly = true)
    public boolean existsByGroupIdAndMemberUserId(Long groupId, Long memberUserId) {
        return groupMemberRepository.existsByIdGroupIdAndIdMemberUserId(groupId, memberUserId);
    }

    /**
     * Counts members of a group.
     *
     * @param groupId group primary key
     * @return member count
     */
    @Transactional(readOnly = true)
    public long countByGroupId(Long groupId) {
        return groupMemberRepository.countByIdGroupId(groupId);
    }

    /**
     * Counts members of a group with no grade assigned.
     *
     * @param groupId group primary key
     * @return count of members without a grade
     */
    @Transactional(readOnly = true)
    public long countMembersWithoutGrade(Long groupId) {
        return groupMemberRepository.countMembersWithoutGrade(groupId);
    }

    /**
     * Batches member counts for several groups at once, keyed by group id.
     *
     * @param groupIds group primary keys
     * @return member count per group id; groups with zero members are absent from the map
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> countByGroupIdBatch(Collection<Long> groupIds) {
        return toCountMap(groupIds, groupMemberRepository::countByIdGroupIdIn);
    }

    /**
     * Batches no-grade member counts for several groups at once, keyed by group id.
     *
     * @param groupIds group primary keys
     * @return no-grade member count per group id; groups with none are absent from the map
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> countMembersWithoutGradeBatch(Collection<Long> groupIds) {
        return toCountMap(groupIds, groupMemberRepository::countMembersWithoutGradeByIdGroupIdIn);
    }

    Map<Long, Long> toCountMap(Collection<Long> groupIds, java.util.function.Function<Collection<Long>, List<Object[]>> query) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : query.apply(groupIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    /**
     * Counts distinct users who are a member of at least one group in the workspace.
     *
     * @return distinct member-user count
     */
    @Transactional(readOnly = true)
    public long countDistinctMemberUsers() {
        return groupMemberRepository.countDistinctMemberUsers();
    }

    /**
     * Counts distinct users who are a member of at least one of the given groups.
     *
     * @param groupIds group primary keys
     * @return distinct member-user count, or {@code 0} when {@code groupIds} is empty
     */
    @Transactional(readOnly = true)
    public long countDistinctMemberUsers(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }
        return groupMemberRepository.countDistinctMemberUsersByIdGroupIdIn(groupIds);
    }

    /**
     * Persists a group membership.
     *
     * @param groupMember the membership to save
     * @return the saved {@link GroupMember}
     */
    @Transactional
    public GroupMember save(GroupMember groupMember) {
        return groupMemberRepository.save(groupMember);
    }

    /**
     * Deletes a group membership by its composite id.
     *
     * @param id the composite group-member id
     */
    @Transactional
    public void deleteById(GroupMember.GroupMemberId id) {
        groupMemberRepository.deleteById(id);
    }

    /**
     * Checks whether a group membership exists by its composite id.
     *
     * @param id the composite group-member id
     * @return {@code true} when the membership exists
     */
    @Transactional(readOnly = true)
    public boolean existsById(GroupMember.GroupMemberId id) {
        return groupMemberRepository.existsById(id);
    }
}
