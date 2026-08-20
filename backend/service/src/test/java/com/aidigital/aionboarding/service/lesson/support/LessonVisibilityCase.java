package com.aidigital.aionboarding.service.lesson.support;

/**
 * One row of the shared lesson-visibility test matrix, consumed by both
 * {@link LessonVisibilityPolicyTest} (exercising the real rule) and
 * {@code LessonActivityServiceImplTest#canViewLesson} (exercising delegation to the same rule),
 * so the two call sites cannot silently drift apart.
 *
 * @param description           human-readable case label, used as the parameterized test name
 * @param admin                 whether the viewer is an admin
 * @param lessonsManage         whether the viewer holds the {@code LESSONS_MANAGE} permission
 * @param authorIsViewer        whether the lesson's {@code createdByUser} is the viewer
 * @param publicationStatusCode the lesson's {@code publicationStatus} code
 * @param enrolled              whether the viewer holds an existing enrollment for the lesson
 * @param expectedVisible       the expected visibility outcome for this combination
 */
public record LessonVisibilityCase(
    String description,
    boolean admin,
    boolean lessonsManage,
    boolean authorIsViewer,
    String publicationStatusCode,
    boolean enrolled,
    boolean expectedVisible
) {

    @Override
    public String toString() {
        return description;
    }
}
