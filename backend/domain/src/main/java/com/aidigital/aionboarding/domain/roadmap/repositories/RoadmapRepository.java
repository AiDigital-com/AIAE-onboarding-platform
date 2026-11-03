package com.aidigital.aionboarding.domain.roadmap.repositories;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long>, JpaSpecificationExecutor<Roadmap> {

	List<Roadmap> findByAutoAssignNewUsersTrue();
}
