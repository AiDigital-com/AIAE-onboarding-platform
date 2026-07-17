package com.aidigital.aionboarding.service.group.services.entity;

import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.repositories.GroupLeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Short-transaction CRUD helpers for the {@link GroupLead} entity.
 * <p>
 * This is the only service that may inject {@link GroupLeadRepository} directly. All other
 * services that require group-lead data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class GroupLeadEntityService {

	private final GroupLeadRepository groupLeadRepository;

	/**
	 * Loads every lead assignment for the given groups, with lead users eagerly fetched.
	 *
	 * @param groupIds group primary keys
	 * @return matching lead assignments
	 */
	@Transactional(readOnly = true)
	public List<GroupLead> findByGroupIdIn(Collection<Long> groupIds) {
		if (groupIds == null || groupIds.isEmpty()) {
			return List.of();
		}
		return groupLeadRepository.findByIdGroupIdIn(groupIds);
	}

	/**
	 * Loads the ids of every group the given user leads.
	 *
	 * @param leadUserId lead user primary key
	 * @return matching group ids
	 */
	@Transactional(readOnly = true)
	public Set<Long> findGroupIdsByLeadUserId(Long leadUserId) {
		return groupLeadRepository.findGroupIdsByIdLeadUserId(leadUserId);
	}

	/**
	 * Checks whether a user leads a specific group.
	 *
	 * @param groupId    group primary key
	 * @param leadUserId lead user primary key
	 * @return {@code true} when the lead assignment exists
	 */
	@Transactional(readOnly = true)
	public boolean existsByGroupIdAndLeadUserId(Long groupId, Long leadUserId) {
		return groupLeadRepository.existsByIdGroupIdAndIdLeadUserId(groupId, leadUserId);
	}

	/**
	 * Counts leads assigned to a group.
	 *
	 * @param groupId group primary key
	 * @return number of leads
	 */
	@Transactional(readOnly = true)
	public long countByGroupId(Long groupId) {
		return groupLeadRepository.countByIdGroupId(groupId);
	}

	/**
	 * Counts distinct users who lead at least one group in the workspace.
	 *
	 * @return distinct lead-user count
	 */
	@Transactional(readOnly = true)
	public long countDistinctLeadUsers() {
		return groupLeadRepository.countDistinctLeadUsers();
	}

	/**
	 * Counts distinct users who lead at least one of the given groups.
	 *
	 * @param groupIds group primary keys
	 * @return distinct lead-user count, or {@code 0} when {@code groupIds} is empty
	 */
	@Transactional(readOnly = true)
	public long countDistinctLeadUsers(Collection<Long> groupIds) {
		if (groupIds == null || groupIds.isEmpty()) {
			return 0;
		}
		return groupLeadRepository.countDistinctLeadUsersByIdGroupIdIn(groupIds);
	}

	/**
	 * Persists a group-lead assignment.
	 *
	 * @param groupLead the assignment to save
	 * @return the saved {@link GroupLead}
	 */
	@Transactional
	public GroupLead save(GroupLead groupLead) {
		return groupLeadRepository.save(groupLead);
	}

	/**
	 * Deletes a group-lead assignment by its composite id.
	 *
	 * @param id the composite group-lead id
	 */
	@Transactional
	public void deleteById(GroupLead.GroupLeadId id) {
		groupLeadRepository.deleteById(id);
	}

	/**
	 * Checks whether a group-lead assignment exists by its composite id.
	 *
	 * @param id the composite group-lead id
	 * @return {@code true} when the assignment exists
	 */
	@Transactional(readOnly = true)
	public boolean existsById(GroupLead.GroupLeadId id) {
		return groupLeadRepository.existsById(id);
	}
}
