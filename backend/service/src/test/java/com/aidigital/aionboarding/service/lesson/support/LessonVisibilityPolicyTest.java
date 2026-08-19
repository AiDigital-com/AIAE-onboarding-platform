package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonPublicationStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonVisibilityPolicyTest {

	@Mock
	private PermissionService permissionService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;

	@InjectMocks
	private LessonVisibilityPolicy policy;

	/**
	 * Shared visibility matrix. Also consumed by
	 * {@code LessonActivityServiceImplTest#canViewLesson} so the real rule (exercised here
	 * against the actual {@link LessonVisibilityPolicy}) and its delegation call site cannot
	 * drift out of sync. Declared {@code public static} so a {@code @MethodSource} in the other
	 * (different-package) test class can reference it by fully-qualified name.
	 *
	 * @return the case matrix
	 */
	public static Stream<LessonVisibilityCase> visibilityMatrix() {
		return Stream.of(
				new LessonVisibilityCase("admin sees an archived lesson",
						true, false, false, LessonPublicationStatusCode.ARCHIVED, true, true),
				new LessonVisibilityCase("LESSONS_MANAGE holder sees their own private lesson",
						false, true, true, LessonPublicationStatusCode.PRIVATE, false, true),
				new LessonVisibilityCase("published lesson is visible to a plain learner",
						false, false, false, LessonPublicationStatusCode.PUBLISHED, false, true),
				new LessonVisibilityCase("private lesson is visible when the viewer is enrolled",
						false, false, false, LessonPublicationStatusCode.PRIVATE, true, true),
				new LessonVisibilityCase("private lesson is hidden when the viewer is not enrolled",
						false, false, false, LessonPublicationStatusCode.PRIVATE, false, false),
				new LessonVisibilityCase("archived lesson is hidden even when the viewer is enrolled",
						false, false, false, LessonPublicationStatusCode.ARCHIVED, true, false)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("visibilityMatrix")
	void isVisible_matchesExpectedOutcomeTest(LessonVisibilityCase visibilityCase) {
		// Given:
		AppUser viewer = viewer(visibilityCase.admin());
		Long authorId = visibilityCase.authorIsViewer() ? viewer.internalId() : 999L;
		Lesson lesson = lesson(10L, visibilityCase.publicationStatusCode(), authorId);
		lenient().when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE))
				.thenReturn(visibilityCase.lessonsManage());
		lenient().when(permissionService.canManageExistingLesson(viewer, authorId))
				.thenReturn(visibilityCase.lessonsManage() && visibilityCase.authorIsViewer());
		lenient().when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(
						List.of(viewer.internalId()), List.of(10L)))
				.thenReturn(visibilityCase.enrolled()
						? List.of(userLesson(viewer.internalId(), 10L))
						: List.of());

		// When:
		boolean result = policy.isVisible(viewer, lesson);

		// Then:
		assertThat(result).isEqualTo(visibilityCase.expectedVisible());
	}

	@Test
	void visibleLessonIds_adminBypassesWithoutAnyPermissionOrEnrollmentQueryTest() {
		// Given: an admin checking a batch of lessons in various states
		AppUser admin = viewer(true);
		Lesson published = lesson(1L, LessonPublicationStatusCode.PUBLISHED, 999L);
		Lesson archived = lesson(2L, LessonPublicationStatusCode.ARCHIVED, 999L);

		// When:
		Set<Long> visible = policy.visibleLessonIds(admin, List.of(published, archived));

		// Then:
		assertThat(visible).containsExactlyInAnyOrder(1L, 2L);
		verifyNoInteractions(permissionService, learningEnrollmentEntityService);
	}

	@Test
	void visibleLessonIds_issuesExactlyOneEnrollmentQueryWithAllPrivateLessonIdsTest() {
		// Given: five private lessons, none authored by the viewer, viewer holds no manage
		// permission — a naive per-lesson implementation would issue five enrollment queries
		AppUser viewer = viewer(false);
		List<Lesson> lessons = List.of(
				lesson(1L, LessonPublicationStatusCode.PRIVATE, 999L),
				lesson(2L, LessonPublicationStatusCode.PRIVATE, 999L),
				lesson(3L, LessonPublicationStatusCode.PRIVATE, 999L),
				lesson(4L, LessonPublicationStatusCode.PRIVATE, 999L),
				lesson(5L, LessonPublicationStatusCode.PRIVATE, 999L)
		);
		when(permissionService.userHasPermission(viewer, PermissionKeys.LESSONS_MANAGE)).thenReturn(false);
		when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(
				List.of(viewer.internalId()), List.of(1L, 2L, 3L, 4L, 5L)))
				.thenReturn(List.of(userLesson(viewer.internalId(), 2L), userLesson(viewer.internalId(), 4L)));

		// When:
		Set<Long> visible = policy.visibleLessonIds(viewer, lessons);

		// Then:
		assertThat(visible).containsExactlyInAnyOrder(2L, 4L);
		verify(learningEnrollmentEntityService, times(1))
				.findUserLessonsByUserIdsAndLessonIds(List.of(viewer.internalId()), List.of(1L, 2L, 3L, 4L, 5L));
	}

	@Test
	void visibleLessonIds_returnsEmptySetForEmptyInputWithoutAnyQueryTest() {
		// Given:
		AppUser viewer = viewer(false);

		// When:
		Set<Long> visible = policy.visibleLessonIds(viewer, List.of());

		// Then:
		assertThat(visible).isEmpty();
		verifyNoInteractions(permissionService, learningEnrollmentEntityService);
	}

	@Test
	void isVisible_teamLeadCannotSeeAnotherAuthorsPrivateLessonTest() {
		// Given: the concrete leak scenario this policy exists to close — a team lead who is
		// neither admin nor the lesson's author, and holds no enrollment, must not see another
		// author's private lesson (and therefore must not be able to add it to a roadmap by ID)
		AppUser teamLead = new AppUser(3L, "clerk-3", "lead@test.com", "Lead", "teamlead", "Lead", null, null, null);
		Lesson lesson = lesson(42L, LessonPublicationStatusCode.PRIVATE, 999L);
		when(permissionService.userHasPermission(teamLead, PermissionKeys.LESSONS_MANAGE)).thenReturn(true);
		when(permissionService.canManageExistingLesson(teamLead, 999L)).thenReturn(false);
		when(learningEnrollmentEntityService.findUserLessonsByUserIdsAndLessonIds(
				List.of(teamLead.internalId()), List.of(42L)))
				.thenReturn(List.of());

		// When:
		boolean result = policy.isVisible(teamLead, lesson);

		// Then:
		assertThat(result).isFalse();
	}

	private AppUser viewer(boolean admin) {
		String roleCode = admin ? "admin" : "learner";
		return new AppUser(1L, "clerk-1", "user@test.com", "User", roleCode, "User", null, null, null);
	}

	private Lesson lesson(Long id, String publicationStatusCode, Long authorId) {
		Lesson lesson = mock(Lesson.class);
		LessonPublicationStatus status = new LessonPublicationStatus();
		status.setCode(publicationStatusCode);
		lenient().when(lesson.getId()).thenReturn(id);
		lenient().when(lesson.getPublicationStatus()).thenReturn(status);
		User author = new User();
		author.setId(authorId);
		lenient().when(lesson.getCreatedByUser()).thenReturn(author);
		return lesson;
	}

	private UserLesson userLesson(Long userId, Long lessonId) {
		UserLesson userLesson = new UserLesson();
		UserLesson.UserLessonId id = new UserLesson.UserLessonId();
		id.setUserId(userId);
		id.setLessonId(lessonId);
		userLesson.setId(id);
		return userLesson;
	}
}
