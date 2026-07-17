package com.aidigital.aionboarding.service.common.dictionary.services.entity;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short-transaction lookup helpers for {@link LessonStatus}.
 * <p>
 * This is the only service that may inject {@link LessonStatusRepository} directly.
 */
@Service
@RequiredArgsConstructor
public class LessonStatusEntityService {

    private final LessonStatusRepository lessonStatusRepository;

    /**
     * Loads a lesson status by code.
     *
     * @param code lesson status code
     * @return matching lesson status
     * @throws AppException C001 if the code does not exist
     */
    @Transactional(readOnly = true)
    public LessonStatus getReferenceByCode(String code) {
        return lessonStatusRepository.findByCode(code)
            .orElseThrow(() -> new AppException(ErrorReason.C001, "lesson_status:" + code));
    }
}
