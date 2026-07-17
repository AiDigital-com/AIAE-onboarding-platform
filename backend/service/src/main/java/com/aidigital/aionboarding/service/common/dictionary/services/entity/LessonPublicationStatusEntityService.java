package com.aidigital.aionboarding.service.common.dictionary.services.entity;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short-transaction lookup helpers for {@link LessonPublicationStatus}.
 * <p>
 * This is the only service that may inject {@link LessonPublicationStatusRepository} directly.
 */
@Service
@RequiredArgsConstructor
public class LessonPublicationStatusEntityService {

    private final LessonPublicationStatusRepository lessonPublicationStatusRepository;

    /**
     * Loads a lesson publication status by code.
     *
     * @param code lesson publication status code
     * @return matching lesson publication status
     * @throws AppException C001 if the code does not exist
     */
    @Transactional(readOnly = true)
    public LessonPublicationStatus getReferenceByCode(String code) {
        return lessonPublicationStatusRepository.findByCode(code)
            .orElseThrow(() -> new AppException(ErrorReason.C001, "lesson_publication_status:" + code));
    }
}
