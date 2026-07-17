package com.aidigital.aionboarding.domain.roadmap.repositories;

import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long>, JpaSpecificationExecutor<Roadmap> {
}
