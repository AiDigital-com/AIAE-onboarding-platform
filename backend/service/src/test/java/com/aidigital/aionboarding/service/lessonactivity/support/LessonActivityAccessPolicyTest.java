package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.common.dictionary.ActivityTypeCode;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonActivityAccessPolicyTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private DictionaryLookupService dictionaryLookupService;
	@Mock
	private PermissionService permissionService;

	private final LessonActivityPayloadAssembler payloadAssembler = new LessonActivityPayloadAssembler();

	private LessonActivityAccessPolicy accessPolicy() {
		return new LessonActivityAccessPolicy(
				lessonEntityService,
				learningEnrollmentEntityService,
				dictionaryLookupService,
				permissionService,
				payloadAssembler
		);
	}

	@Nested
	class canSeeQuizAnswersForLesson {

		@Test
		void adminAlwaysSeesAnswersRegardlessOfCreatorTest() {
			// Given: an admin viewer and a lesson created by someone else
			AppUser admin = appUser(1L);
			Long lessonCreatedByUserId = 99L;
			when(permissionService.canManageExistingLesson(admin, lessonCreatedByUserId)).thenReturn(true);

			// Execution
			boolean result = accessPolicy().canSeeQuizAnswersForLesson(admin, lessonCreatedByUserId);

			// Verification
			assertThat(result).isTrue();
		}

		@Test
		void teamLeadWhoCreatedTheLessonSeesAnswersTest() {
			// Given: the viewer is the lesson's own creator
			AppUser teamLead = appUser(5L);
			Long lessonCreatedByUserId = 5L;
			when(permissionService.canManageExistingLesson(teamLead, lessonCreatedByUserId)).thenReturn(true);

			// Execution
			boolean result = accessPolicy().canSeeQuizAnswersForLesson(teamLead, lessonCreatedByUserId);

			// Verification
			assertThat(result).isTrue();
		}

		@Test
		void teamLeadWhoDidNotCreateTheLessonDoesNotSeeAnswersTest() {
			// Given: a team lead with broad lesson-management permission, but this lesson
			// belongs to a different author — reproduces the reported bug where any team lead
			// with lessons.manage could see every lesson's quiz answers.
			AppUser teamLead = appUser(5L);
			Long lessonCreatedByUserId = 99L;
			when(permissionService.canManageExistingLesson(teamLead, lessonCreatedByUserId)).thenReturn(false);

			// Execution
			boolean result = accessPolicy().canSeeQuizAnswersForLesson(teamLead, lessonCreatedByUserId);

			// Verification
			assertThat(result).isFalse();
		}
	}

	@Nested
	class redactQuizAnswersUnlessManager {

		@Test
		void stripsCorrectAnswerKeysWhenViewerMayNotSeeThemTest() {
			// Given: a quiz activity and a viewer who is not the creator/admin
			AppUser member = appUser(7L);
			Long lessonCreatedByUserId = 99L;
			when(permissionService.canManageExistingLesson(member, lessonCreatedByUserId)).thenReturn(false);

			LessonActivityRecord activity = quizActivity();

			// Execution
			LessonActivityRecord redacted = accessPolicy()
					.redactQuizAnswersUnlessManager(activity, member, lessonCreatedByUserId);

			// Verification
			List<Map<String, Object>> items = payloadAssembler.asMapList(redacted.payload().get("items"));
			assertThat(items).hasSize(1);
			assertThat(items.get(0)).doesNotContainKeys("correctAnswer", "correctAnswers");
			assertThat(items.get(0)).containsEntry("question", "What is broad match?");
		}

		@Test
		void keepsCorrectAnswerKeysWhenViewerIsTheCreatorTest() {
			// Given: the viewer created this lesson
			AppUser creator = appUser(7L);
			Long lessonCreatedByUserId = 7L;
			when(permissionService.canManageExistingLesson(creator, lessonCreatedByUserId)).thenReturn(true);

			LessonActivityRecord activity = quizActivity();

			// Execution
			LessonActivityRecord result = accessPolicy()
					.redactQuizAnswersUnlessManager(activity, creator, lessonCreatedByUserId);

			// Verification
			List<Map<String, Object>> items = payloadAssembler.asMapList(result.payload().get("items"));
			assertThat(items.get(0)).containsEntry("correctAnswer", "Paris");
		}

		@Test
		void leavesNonQuizActivitiesUntouchedTest() {
			// Given: a flashcards activity and a viewer who may not see quiz answers
			AppUser member = appUser(7L);
			Long lessonCreatedByUserId = 99L;

			LessonActivityRecord activity = new LessonActivityRecord(
					1L, 10L, ActivityTypeCode.FLASHCARDS, "Flashcards", 1,
					Map.of("cards", List.of(Map.of("front", "Q", "back", "A"))),
					Map.of(), "author", null, null
			);

			// Execution
			LessonActivityRecord result = accessPolicy()
					.redactQuizAnswersUnlessManager(activity, member, lessonCreatedByUserId);

			// Verification
			assertThat(result).isSameAs(activity);
		}
	}

	private AppUser appUser(long internalId) {
		return new AppUser(internalId, "clerk-" + internalId, "user" + internalId + "@test.com",
				"User " + internalId, "teamlead", "User", null, null, null);
	}

	private LessonActivityRecord quizActivity() {
		Map<String, Object> item = new java.util.LinkedHashMap<>();
		item.put("type", "single_choice");
		item.put("question", "What is broad match?");
		item.put("options", List.of("Paris", "Berlin"));
		item.put("correctAnswer", "Paris");
		item.put("explanation", "Because.");
		return new LessonActivityRecord(
				1L, 10L, ActivityTypeCode.QUIZ, "Quiz", 1,
				Map.of("items", List.of(item)),
				Map.of(), "author", null, null
		);
	}
}
