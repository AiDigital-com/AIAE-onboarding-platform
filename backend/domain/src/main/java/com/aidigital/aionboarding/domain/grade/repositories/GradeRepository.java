package com.aidigital.aionboarding.domain.grade.repositories;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    List<Grade> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<Grade> findAllByOrderByDisplayOrderAsc();
}
