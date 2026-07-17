package com.aidigital.aionboarding.service.group.services.entity;

import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.repositories.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Short-transaction CRUD helpers for the {@link Group} entity.
 * <p>
 * This is the only service that may inject {@link GroupRepository} directly. All other services
 * that require group data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class GroupEntityService {

    private final GroupRepository groupRepository;

    /**
     * Loads a group by primary key.
     *
     * @param id group primary key
     * @return matching group, when present
     */
    @Transactional(readOnly = true)
    public Optional<Group> findById(Long id) {
        return groupRepository.findById(id);
    }

    /**
     * Loads a group by primary key or throws when missing.
     *
     * @param id group primary key
     * @return matching group
     */
    @Transactional(readOnly = true)
    public Group getReference(Long id) {
        return groupRepository.getReferenceById(id);
    }

    /**
     * Searches groups with the given specification and pagination.
     *
     * @param specification search predicate
     * @param page          zero-based page index
     * @param size          page size
     * @return matching page of groups
     */
    @Transactional(readOnly = true)
    public Page<Group> search(Specification<Group> specification, int page, int size) {
        return groupRepository.findAll(specification, PageRequest.of(page, size));
    }

    /**
     * Counts every group in the workspace.
     *
     * @return total group count
     */
    @Transactional(readOnly = true)
    public long count() {
        return groupRepository.count();
    }

    /**
     * Checks whether a group name is already in use, case-insensitively.
     *
     * @param normalizedName lower-cased, trimmed candidate name
     * @return {@code true} when a group already uses this name
     */
    @Transactional(readOnly = true)
    public boolean existsByNormalizedName(String normalizedName) {
        return groupRepository.existsByNormalizedName(normalizedName);
    }

    /**
     * Checks whether a group name is already in use by a different group, case-insensitively.
     *
     * @param normalizedName lower-cased, trimmed candidate name
     * @param excludingId    group id to exclude from the check (the group being renamed)
     * @return {@code true} when a different group already uses this name
     */
    @Transactional(readOnly = true)
    public boolean existsByNormalizedNameExcluding(String normalizedName, Long excludingId) {
        return groupRepository.existsByNormalizedNameAndIdNot(normalizedName, excludingId);
    }

    /**
     * Persists a group.
     *
     * @param group the group to save
     * @return the saved {@link Group}
     */
    @Transactional
    public Group save(Group group) {
        return groupRepository.save(group);
    }

    /**
     * Deletes a group.
     *
     * @param group the group to delete
     */
    @Transactional
    public void delete(Group group) {
        groupRepository.delete(group);
    }
}
