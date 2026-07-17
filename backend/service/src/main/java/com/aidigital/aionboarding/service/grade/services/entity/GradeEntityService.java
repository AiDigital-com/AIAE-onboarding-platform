package com.aidigital.aionboarding.service.grade.services.entity;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import com.aidigital.aionboarding.domain.grade.repositories.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Short-transaction CRUD helpers for the {@link Grade} entity.
 * <p>
 * This is the only service that may inject {@link GradeRepository} directly. All other services
 * that require grade data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class GradeEntityService {

    private final GradeRepository gradeRepository;

    /**
     * Loads a grade by primary key.
     *
     * @param id grade primary key
     * @return matching grade, when present
     */
    @Transactional(readOnly = true)
    public Optional<Grade> findById(Long id) {
        return gradeRepository.findById(id);
    }

    /**
     * Loads a grade by its unique code.
     *
     * @param code grade code
     * @return matching grade, when present
     */
    @Transactional(readOnly = true)
    public Optional<Grade> findByCode(String code) {
        return gradeRepository.findByCode(code);
    }

    /**
     * Checks whether a grade code is already in use, case-insensitively.
     *
     * @param code candidate code
     * @return {@code true} when a grade already uses this code
     */
    @Transactional(readOnly = true)
    public boolean existsByCodeIgnoreCase(String code) {
        return gradeRepository.existsByCodeIgnoreCase(code);
    }

    /**
     * Checks whether a grade name is already in use, case-insensitively.
     *
     * @param name candidate name
     * @return {@code true} when a grade already uses this name
     */
    @Transactional(readOnly = true)
    public boolean existsByNameIgnoreCase(String name) {
        return gradeRepository.existsByNameIgnoreCase(name);
    }

    /**
     * Loads every active grade, ordered for display.
     *
     * @return active grades in display order
     */
    @Transactional(readOnly = true)
    public List<Grade> findActiveOrderByDisplayOrder() {
        return gradeRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Loads every grade including inactive ones, ordered for display.
     *
     * @return all grades in display order
     */
    @Transactional(readOnly = true)
    public List<Grade> findAllOrderByDisplayOrder() {
        return gradeRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Persists a grade.
     *
     * @param grade the grade to save
     * @return the saved {@link Grade}
     */
    @Transactional
    public Grade save(Grade grade) {
        return gradeRepository.save(grade);
    }
}
