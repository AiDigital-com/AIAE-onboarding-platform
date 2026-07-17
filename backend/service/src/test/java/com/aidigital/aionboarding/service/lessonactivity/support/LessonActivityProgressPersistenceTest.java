package com.aidigital.aionboarding.service.lessonactivity.support;

import com.aidigital.aionboarding.domain.lessonactivity.entities.LessonActivity;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityAttempt;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityAttemptRepository;
import com.aidigital.aionboarding.domain.lessonactivity.repositories.UserLessonActivityProgressRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.learning.services.entity.LearningEnrollmentEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonActivityProgressPersistenceTest {

	@Mock
	private UserLessonActivityProgressRepository progressRepository;
	@Mock
	private UserLessonActivityAttemptRepository attemptRepository;
	@Mock
	private LearningEnrollmentEntityService learningEnrollmentEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonActivityAccessPolicy accessPolicy;
	@Mock
	private CurrentTime currentTime;

	@InjectMocks
	private LessonActivityProgressPersistence persistence;

	@Nested
	class InsertAttemptWithNextNumber {

		@Test
		void shouldAssignTheNextNumberForTheEntitysUserAndActivityThenSaveTest() {
			// Given:
			User user = new User();
			user.setId(7L);
			LessonActivity activity = new LessonActivity();
			activity.setId(9L);
			UserLessonActivityAttempt attempt = new UserLessonActivityAttempt();
			attempt.setUser(user);
			attempt.setActivity(activity);
			when(attemptRepository.nextAttemptNumber(eq(7L), eq(9L))).thenReturn(3);
			when(attemptRepository.save(attempt)).thenReturn(attempt);

			// When:
			UserLessonActivityAttempt result = persistence.insertAttemptWithNextNumber(attempt);

			// Then:
			assertThat(result).isSameAs(attempt);
			assertThat(attempt.getAttemptNumber()).isEqualTo(3);
		}
	}
}
