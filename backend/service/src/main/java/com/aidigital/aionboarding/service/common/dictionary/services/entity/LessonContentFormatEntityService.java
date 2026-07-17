package com.aidigital.aionboarding.service.common.dictionary.services.entity;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short-transaction lookup helpers for {@link LessonContentFormat}.
 * <p>
 * This is the only service that may inject {@link LessonContentFormatRepository} directly.
 */
@Service
@RequiredArgsConstructor
public class LessonContentFormatEntityService {

    private final LessonContentFormatRepository lessonContentFormatRepository;

    /**
     * Loads a lesson content format by code.
     *
     * @param code lesson content format code
     * @return matching lesson content format
     * @throws AppException C001 if the code does not exist
     */
    @Transactional(readOnly = true)
    public LessonContentFormat getReferenceByCode(String code) {
        return lessonContentFormatRepository.findByCode(code)
            .orElseThrow(() -> new AppException(ErrorReason.C001, "lesson_content_format:" + code));
    }
}
