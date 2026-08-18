package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.LessonAssistantConversation;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.enums.LessonAssistantPreset;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssistantConversationEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonAssistantAnswerWorkflow;
import com.aidigital.aionboarding.service.lesson.support.LessonAssistantConversationAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonAssistantServiceImplTest {

	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private LessonAssistantConversationEntityService lessonAssistantConversationEntityService;
	@Mock
	private LessonAssistantConversationAssembler lessonAssistantConversationAssembler;
	@Mock
	private LessonAssistantAnswerWorkflow lessonAssistantAnswerWorkflow;

	@InjectMocks
	private LessonAssistantServiceImpl service;

	private AppUser viewer() {
		return new AppUser(1L, "clerk-1", "user@example.com", "Test User", "learner", "User", null, null, null);
	}

	@Nested
	class Ask {

		@Test
		void shouldRequirePermissionAndDelegateToWorkflowTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 10L;
			AskLessonResultRecord workflowResult = new AskLessonResultRecord("answer", java.util.Map.of());
			when(lessonAssistantAnswerWorkflow.ask(viewer, lessonId, "question", List.of(), LessonAssistantPreset.REGULAR))
					.thenReturn(workflowResult);

			// When:
			AskLessonResultRecord result =
					service.ask(viewer, lessonId, "question", List.of(), LessonAssistantPreset.REGULAR);

			// Then:
			assertThat(result).isSameAs(workflowResult);
			verify(permissionService).requirePermission(viewer, PermissionKeys.LEARNING_ASK);
		}
	}

	@Nested
	class GetConversation {

		@Test
		void getConversationWhenNoneSaved_shouldReturnEmptyConversationTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 70L;
			when(learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.of(mock(com.aidigital.aionboarding.domain.learning.entities.UserLesson.class)));
			when(lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.empty());

			// When:
			var result = service.getConversation(viewer, lessonId);

			// Then:
			assertThat(result.messages()).isEmpty();
			assertThat(result.preset()).isEqualTo("regular");
		}

		@Test
		void getConversationWhenSaved_shouldReturnAssembledRecordTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 71L;
			var conversation = new LessonAssistantConversation();
			var expectedRecord = new LessonAssistantConversationRecord(
					List.of(new com.aidigital.aionboarding.service.lesson.models.ChatTurn("user", "Hi")), "regular");
			when(learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.of(mock(com.aidigital.aionboarding.domain.learning.entities.UserLesson.class)));
			when(lessonAssistantConversationEntityService.findByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.of(conversation));
			when(lessonAssistantConversationAssembler.toRecord(conversation)).thenReturn(expectedRecord);

			// When:
			var result = service.getConversation(viewer, lessonId);

			// Then:
			assertThat(result).isSameAs(expectedRecord);
		}

		@Test
		void getConversationWhenNotEnrolled_shouldThrowTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 72L;
			when(learningEnrollmentEntityService.findUserLessonByUserIdAndLessonId(viewer.internalId(), lessonId))
					.thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.getConversation(viewer, lessonId))
					.isInstanceOf(AppException.class);
		}
	}

	@Nested
	class ClearConversation {

		@Test
		void clearConversation_shouldDeleteTheLearnersConversationTest() {
			// Given:
			AppUser viewer = viewer();
			Long lessonId = 80L;

			// When:
			service.clearConversation(viewer, lessonId);

			// Then:
			verify(lessonAssistantConversationEntityService)
					.deleteByUserIdAndLessonId(viewer.internalId(), lessonId);
		}
	}
}
