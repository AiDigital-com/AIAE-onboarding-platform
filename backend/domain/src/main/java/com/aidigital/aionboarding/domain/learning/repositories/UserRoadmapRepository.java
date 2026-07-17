package com.aidigital.aionboarding.domain.learning.repositories;

import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserRoadmapRepository
    extends JpaRepository<UserRoadmap, UserRoadmap.UserRoadmapId>, UserRoadmapRepositoryCustom {

    List<UserRoadmap> findByIdUserId(Long userId);

    List<UserRoadmap> findByIdRoadmapId(Long roadmapId);

    @Query("""
        SELECT ur
        FROM UserRoadmap ur
        JOIN FETCH ur.user
        WHERE ur.id.roadmapId = :roadmapId
        ORDER BY ur.enrolledAt DESC
        """)
    List<UserRoadmap> findByRoadmapIdWithUser(@Param("roadmapId") Long roadmapId);

    @Query("""
        SELECT ur
        FROM UserRoadmap ur
        WHERE ur.id.userId = :userId
          AND ur.id.roadmapId IN :roadmapIds
        """)
    List<UserRoadmap> findByUserIdAndRoadmapIds(
        @Param("userId") Long userId,
        @Param("roadmapIds") Collection<Long> roadmapIds
    );

    @Query("""
        SELECT ur
        FROM UserRoadmap ur
        WHERE ur.id.userId IN :userIds
          AND ur.id.roadmapId = :roadmapId
        """)
    List<UserRoadmap> findByUserIdsAndRoadmapId(
        @Param("userIds") Collection<Long> userIds,
        @Param("roadmapId") Long roadmapId
    );

    /**
     * Bulk-deletes the direct roadmap enrollment rows for a set of users and one roadmap.
     *
     * @param userIds   users whose roadmap enrollment is being revoked
     * @param roadmapId roadmap being revoked
     * @return number of deleted roadmap-enrollment rows
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserRoadmap ur WHERE ur.id.userId IN :userIds AND ur.id.roadmapId = :roadmapId")
    int deleteByUserIdsAndRoadmapId(@Param("userIds") Collection<Long> userIds, @Param("roadmapId") Long roadmapId);
}
