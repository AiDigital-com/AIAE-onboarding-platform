package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.LessonStatusCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.lesson.prompt.LessonRevisionPromptBuilder;
import com.aidigital.aionboarding.service.lesson.support.LessonRevisionRequestValidator.ValidatedRevisionRequest;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRevisionRequestValidatorTest {

	@Mock
	private PermissionService permissionService;
	@Mock
	private LessonRevisionPromptBuilder lessonRevisionPromptBuilder;

	@InjectMocks
	private LessonRevisionRequestValidator validator;

	@Test
	void shouldValidateRevisionRequestWithFreeformTextTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "admin", "Viewer", null, null, null);
		Lesson lesson = readyLesson(10L, 1L);
		ReviseLessonInput input = new ReviseLessonInput("Make it simpler", List.of());
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);

		// When:
		ValidatedRevisionRequest result = validator.validate(viewer, lesson, input);

		// Then:
		assertThat(result.revisionRequest()).isEqualTo("Make it simpler");
		assertThat(result.selectedOptions()).isEmpty();
	}

	@Test
	void shouldValidateRevisionRequestWithSelectedOptionsTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "admin", "Viewer", null, null, null);
		Lesson lesson = readyLesson(11L, null);
		ReviseLessonInput input = new ReviseLessonInput(null, List.of("simpler", "invalid", "simpler"));
		when(permissionService.canManageExistingLesson(eq(viewer), eq(null))).thenReturn(true);
		when(lessonRevisionPromptBuilder.allowedRevisionOptions()).thenReturn(Set.of("simpler", "shorter"));

		// When:
		ValidatedRevisionRequest result = validator.validate(viewer, lesson, input);

		// Then:
		assertThat(result.revisionRequest()).isEmpty();
		assertThat(result.selectedOptions()).containsExactly("simpler");
	}

	@Test
	void shouldRejectWhenViewerCannotManageLessonTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Lesson lesson = readyLesson(12L, 3L);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(3L))).thenReturn(false);

		// When-Then:
		assertThatThrownBy(() -> validator.validate(viewer, lesson, new ReviseLessonInput("x", List.of())))
				.isInstanceOf(AppException.class);
	}

	@Test
	void shouldRejectWhenLessonIsNotReadyTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "admin", "Viewer", null, null, null);
		Lesson lesson = new Lesson();
		lesson.setId(13L);
		LessonStatus status = new LessonStatus();
		status.setCode(LessonStatusCode.DRAFT);
		lesson.setStatus(status);
		lesson.setContentHtml("<p>content</p>");
		User author = new User();
		author.setId(1L);
		lesson.setCreatedByUser(author);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);

		// When-Then:
		assertThatThrownBy(() -> validator.validate(viewer, lesson, new ReviseLessonInput("x", List.of())))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Only ready lessons can be revised");
	}

	@Test
	void shouldRejectWhenLessonContentIsEmptyTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "admin", "Viewer", null, null, null);
		Lesson lesson = readyLesson(14L, 1L);
		lesson.setContentHtml("   ");
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);

		// When-Then:
		assertThatThrownBy(() -> validator.validate(viewer, lesson, new ReviseLessonInput("x", List.of())))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Lesson content is empty");
	}

	@Test
	void shouldRejectWhenRequestAndOptionsAreBlankTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "admin", "Viewer", null, null, null);
		Lesson lesson = readyLesson(15L, 1L);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);
		when(lessonRevisionPromptBuilder.allowedRevisionOptions()).thenReturn(Set.of("simpler"));

		// When-Then:
		assertThatThrownBy(() -> validator.validate(viewer, lesson, new ReviseLessonInput("   ", List.of("unknown"))))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Add revision notes or select at least one revision option");
	}

	private Lesson readyLesson(Long id, Long authorId) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		LessonStatus status = new LessonStatus();
		status.setCode(LessonStatusCode.READY);
		lesson.setStatus(status);
		lesson.setContentHtml("<p>content</p>");
		if (authorId != null) {
			User author = new User();
			author.setId(authorId);
			lesson.setCreatedByUser(author);
		}
		return lesson;
	}
}
