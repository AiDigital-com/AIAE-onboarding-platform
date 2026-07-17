package com.aidigital.aionboarding.service.roadmap.services.entity;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapLessonRepository;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Short-transaction CRUD helpers for the {@link Roadmap} and {@link RoadmapLesson} entities.
 * <p>
 * This is the only service that may inject {@link RoadmapRepository} or
 * {@link RoadmapLessonRepository} directly. All other services that require roadmap data must
 * depend on this service.
 */
@Service
@RequiredArgsConstructor
public class RoadmapEntityService {

    private final RoadmapRepository roadmapRepository;
    private final RoadmapLessonRepository roadmapLessonRepository;
    private final RoadmapSpecificationBuilder roadmapSpecificationBuilder;

    /**
     * Loads a single roadmap by primary key, throwing if it does not exist.
     *
     * @param roadmapId the roadmap primary key
     * @return the {@link Roadmap} entity
     * @throws AppException C001 if no roadmap with the given ID exists
     */
    @Transactional(readOnly = true)
    public Roadmap getReference(Long roadmapId) {
        return roadmapRepository.findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorReason.C001, roadmapId));
    }

    /**
     * Loads a single roadmap by primary key without throwing when it does not exist.
     *
     * @param roadmapId the roadmap primary key
     * @return the {@link Roadmap} entity, if it exists
     */
    @Transactional(readOnly = true)
    public Optional<Roadmap> findById(Long roadmapId) {
        return roadmapRepository.findById(roadmapId);
    }

    /**
     * Searches roadmaps with the given typed filters, returning a bounded, sorted page.
     *
     * @param filter   typed filter and sort parameters
     * @param viewerId the viewer's internal user id, used only when {@code filter.assignedToMe()} is true
     * @param page     zero-based page index
     * @param size     maximum number of roadmaps per page
     * @return a page of {@link Roadmap} entities matching the filter
     */
    @Transactional(readOnly = true)
    public Page<Roadmap> search(RoadmapListQuery filter, Long viewerId, int page, int size) {
        Specification<Roadmap> specification = roadmapSpecificationBuilder.build(filter, viewerId);
        Pageable pageable = PageRequest.of(page, size);
        return roadmapRepository.findAll(specification, pageable);
    }

    /**
     * Counts roadmaps matching the given typed filters, without fetching or paginating any
     * rows — for cheap tab-count display independent of the active Library tab.
     *
     * @param filter   typed filter and sort parameters
     * @param viewerId the viewer's internal user id, used only when {@code filter.assignedToMe()} is true
     * @return the number of roadmaps matching the filter
     */
    @Transactional(readOnly = true)
    public long countRoadmaps(RoadmapListQuery filter, Long viewerId) {
        Specification<Roadmap> specification = roadmapSpecificationBuilder.build(filter, viewerId);
        return roadmapRepository.count(specification);
    }

    /**
     * Persists any changes to the given roadmap entity and returns the saved state.
     *
     * @param roadmap the roadmap entity to persist
     * @return the saved {@link Roadmap}
     */
    @Transactional
    public Roadmap save(Roadmap roadmap) {
        return roadmapRepository.save(roadmap);
    }

    /**
     * Removes the given roadmap entity from the database.
     *
     * @param roadmap the roadmap entity to delete
     */
    @Transactional
    public void delete(Roadmap roadmap) {
        roadmapRepository.delete(roadmap);
    }

    /**
     * Batch-loads roadmap-lesson join rows for a set of roadmaps, eagerly fetching each
     * lesson and its status, ordered by sort order ascending.
     *
     * @param roadmapIds the roadmap primary keys
     * @return every {@link RoadmapLesson} row for the given roadmaps
     */
    @Transactional(readOnly = true)
    public List<RoadmapLesson> findAllByRoadmapIdsWithLessons(List<Long> roadmapIds) {
        return roadmapLessonRepository.findAllByRoadmapIdsWithLessons(roadmapIds);
    }

    /**
     * Loads the roadmap-lesson join rows for a single roadmap, ordered by sort order ascending.
     *
     * @param roadmapId the roadmap primary key
     * @return the {@link RoadmapLesson} rows for the roadmap, in sort order
     */
    @Transactional(readOnly = true)
    public List<RoadmapLesson> findByIdRoadmapIdOrderBySortOrderAsc(Long roadmapId) {
        return roadmapLessonRepository.findByIdRoadmapIdOrderBySortOrderAsc(roadmapId);
    }

    /**
     * Loads the roadmap-lesson join rows that reference a given lesson, across all roadmaps.
     *
     * @param lessonId the lesson primary key
     * @return the {@link RoadmapLesson} rows referencing the lesson
     */
    @Transactional(readOnly = true)
    public List<RoadmapLesson> findByIdLessonId(Long lessonId) {
        return roadmapLessonRepository.findByIdLessonId(lessonId);
    }

    /**
     * Persists a single roadmap-lesson join row.
     *
     * @param roadmapLesson the roadmap-lesson row to save
     * @return the saved {@link RoadmapLesson}
     */
    @Transactional
    public RoadmapLesson saveRoadmapLesson(RoadmapLesson roadmapLesson) {
        return roadmapLessonRepository.save(roadmapLesson);
    }

    /**
     * Deletes every roadmap-lesson join row for a single roadmap.
     *
     * @param roadmapId the roadmap primary key
     */
    @Transactional
    public void deleteByIdRoadmapId(Long roadmapId) {
        roadmapLessonRepository.deleteByIdRoadmapId(roadmapId);
    }
}
