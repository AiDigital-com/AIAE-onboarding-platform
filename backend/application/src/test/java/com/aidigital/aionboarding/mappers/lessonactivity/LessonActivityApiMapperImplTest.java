package com.aidigital.aionboarding.mappers.lessonactivity;

import com.aidigital.aionboarding.api.v1.model.ActivityAttemptV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressDetailV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressResponseV1;
import com.aidigital.aionboarding.api.v1.model.ActivityProgressV1;
import com.aidigital.aionboarding.api.v1.model.ActivityPromptV1;
import com.aidigital.aionboarding.api.v1.model.ActivityResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityDetailResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityLessonV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityTypeV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import com.aidigital.aionboarding.api.v1.model.LessonVisibilityV1;
import com.aidigital.aionboarding.api.v1.model.PreparationStatusV1;
import com.aidigital.aionboarding.api.v1.model.QuizAttemptResultItemV1;
import com.aidigital.aionboarding.api.v1.model.QuizQuestionTypeV1;
import com.aidigital.aionboarding.api.v1.model.UpdateActivityRequestV1;
import com.aidigital.aionboarding.mappers.common.LessonActivityTypeApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonVisibilityApiMapper;
import com.aidigital.aionboarding.mappers.common.PreparationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.QuizQuestionTypeApiMapper;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityAttemptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityProgressViewRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.ActivityPromptRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.GenerateActivityResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityWithAttemptsRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonWithActivitiesRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.QuizAnswerResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.SubmitActivityProgressResultRecord;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityInput;
import com.aidigital.aionboarding.service.lessonactivity.models.UpdateActivityResultRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonActivityApiMapperImplTest {

	@InjectMocks
	private LessonActivityApiMapperImpl lessonActivityApiMapperImpl;

	@Mock
	private PreparationStatusApiMapper preparationStatusApiMapper;

	@Mock
	private LessonActivityTypeApiMapper lessonActivityTypeApiMapper;

	@Mock
	private LessonGenerationStatusApiMapper lessonGenerationStatusApiMapper;

	@Mock
	private LessonVisibilityApiMapper lessonVisibilityApiMapper;

	@Mock
	private QuizQuestionTypeApiMapper quizQuestionTypeApiMapper;

	@BeforeEach
	void setUp() {
		when(preparationStatusApiMapper.mapPreparationStatus(anyString())).thenReturn(Instancio.create(PreparationStatusV1.class));
		when(lessonActivityTypeApiMapper.mapLessonActivityType(anyString())).thenReturn(Instancio.create(LessonActivityTypeV1.class));
		when(lessonGenerationStatusApiMapper.mapLessonGenerationStatus(anyString())).thenReturn(Instancio.create(LessonGenerationStatusV1.class));
		when(lessonVisibilityApiMapper.mapLessonVisibility(anyString())).thenReturn(Instancio.create(LessonVisibilityV1.class));
		when(quizQuestionTypeApiMapper.mapQuizQuestionType(anyString())).thenReturn(Instancio.create(QuizQuestionTypeV1.class));
		when(quizQuestionTypeApiMapper.fromQuizQuestionType(any(QuizQuestionTypeV1.class))).thenReturn("value");
	}

	@Test
	void shouldToUpdateActivityInputUpdateActivityRequestV1Test() {
		// Given:
		UpdateActivityRequestV1 request = Instancio.create(UpdateActivityRequestV1.class);

		// When:
		UpdateActivityInput actualResult = lessonActivityApiMapperImpl.toUpdateActivityInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUpdateActivityInputUpdateActivityRequestV1WithNullTest() {
		// Given:
		UpdateActivityRequestV1 request = null;

		// When:
		UpdateActivityInput actualResult = lessonActivityApiMapperImpl.toUpdateActivityInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityProgressV1ActivityProgressViewRecordTest() {
		// Given:
		ActivityProgressViewRecord progress = Instancio.create(ActivityProgressViewRecord.class);

		// When:
		ActivityProgressV1 actualResult = lessonActivityApiMapperImpl.toActivityProgressV1(progress);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityProgressV1ActivityProgressViewRecordWithNullTest() {
		// Given:
		ActivityProgressViewRecord progress = null;

		// When:
		ActivityProgressV1 actualResult = lessonActivityApiMapperImpl.toActivityProgressV1(progress);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonActivityV1LessonActivityRecordTest() {
		// Given:
		LessonActivityRecord activity = Instancio.create(LessonActivityRecord.class);

		// When:
		LessonActivityV1 actualResult = lessonActivityApiMapperImpl.toLessonActivityV1(activity);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonActivityV1LessonActivityRecordWithNullTest() {
		// Given:
		LessonActivityRecord activity = null;

		// When:
		LessonActivityV1 actualResult = lessonActivityApiMapperImpl.toLessonActivityV1(activity);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityPromptV1ActivityPromptRecordTest() {
		// Given:
		ActivityPromptRecord prompt = Instancio.create(ActivityPromptRecord.class);

		// When:
		ActivityPromptV1 actualResult = lessonActivityApiMapperImpl.toActivityPromptV1(prompt);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityPromptV1ActivityPromptRecordWithNullTest() {
		// Given:
		ActivityPromptRecord prompt = null;

		// When:
		ActivityPromptV1 actualResult = lessonActivityApiMapperImpl.toActivityPromptV1(prompt);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonActivityLessonV1LessonWithActivitiesRecordTest() {
		// Given:
		LessonWithActivitiesRecord lesson = Instancio.create(LessonWithActivitiesRecord.class);

		// When:
		LessonActivityLessonV1 actualResult = lessonActivityApiMapperImpl.toLessonActivityLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonActivityLessonV1LessonWithActivitiesRecordWithNullTest() {
		// Given:
		LessonWithActivitiesRecord lesson = null;

		// When:
		LessonActivityLessonV1 actualResult = lessonActivityApiMapperImpl.toLessonActivityLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityResponseV1GenerateActivityResultRecordTest() {
		// Given:
		GenerateActivityResultRecord result = Instancio.create(GenerateActivityResultRecord.class);

		// When:
		ActivityResponseV1 actualResult = lessonActivityApiMapperImpl.toActivityResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityResponseV1GenerateActivityResultRecordWithNullTest() {
		// Given:
		GenerateActivityResultRecord result = null;

		// When:
		ActivityResponseV1 actualResult = lessonActivityApiMapperImpl.toActivityResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityResponseV1UpdateActivityResultRecordTest() {
		// Given:
		UpdateActivityResultRecord result = Instancio.create(UpdateActivityResultRecord.class);

		// When:
		ActivityResponseV1 actualResult = lessonActivityApiMapperImpl.toActivityResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityResponseV1UpdateActivityResultRecordWithNullTest() {
		// Given:
		UpdateActivityResultRecord result = null;

		// When:
		ActivityResponseV1 actualResult = lessonActivityApiMapperImpl.toActivityResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonActivityDetailResponseV1LessonActivityWithAttemptsRecordTest() {
		// Given:
		LessonActivityWithAttemptsRecord result = Instancio.create(LessonActivityWithAttemptsRecord.class);

		// When:
		LessonActivityDetailResponseV1 actualResult =
				lessonActivityApiMapperImpl.toLessonActivityDetailResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonActivityDetailResponseV1LessonActivityWithAttemptsRecordWithNullTest() {
		// Given:
		LessonActivityWithAttemptsRecord result = null;

		// When:
		LessonActivityDetailResponseV1 actualResult =
				lessonActivityApiMapperImpl.toLessonActivityDetailResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityProgressDetailV1ActivityProgressRecordTest() {
		// Given:
		ActivityProgressRecord progress = Instancio.create(ActivityProgressRecord.class);

		// When:
		ActivityProgressDetailV1 actualResult = lessonActivityApiMapperImpl.toActivityProgressDetailV1(progress);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityProgressDetailV1ActivityProgressRecordWithNullTest() {
		// Given:
		ActivityProgressRecord progress = null;

		// When:
		ActivityProgressDetailV1 actualResult = lessonActivityApiMapperImpl.toActivityProgressDetailV1(progress);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToQuizAttemptResultItemV1QuizAnswerResultRecordTest() {
		// Given:
		QuizAnswerResultRecord result = Instancio.create(QuizAnswerResultRecord.class);

		// When:
		QuizAttemptResultItemV1 actualResult = lessonActivityApiMapperImpl.toQuizAttemptResultItemV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToQuizAttemptResultItemV1QuizAnswerResultRecordWithNullTest() {
		// Given:
		QuizAnswerResultRecord result = null;

		// When:
		QuizAttemptResultItemV1 actualResult = lessonActivityApiMapperImpl.toQuizAttemptResultItemV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityAttemptV1ActivityAttemptRecordTest() {
		// Given:
		ActivityAttemptRecord attempt = Instancio.create(ActivityAttemptRecord.class);

		// When:
		ActivityAttemptV1 actualResult = lessonActivityApiMapperImpl.toActivityAttemptV1(attempt);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityAttemptV1ActivityAttemptRecordWithNullTest() {
		// Given:
		ActivityAttemptRecord attempt = null;

		// When:
		ActivityAttemptV1 actualResult = lessonActivityApiMapperImpl.toActivityAttemptV1(attempt);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToActivityProgressResponseV1SubmitActivityProgressResultRecordTest() {
		// Given:
		SubmitActivityProgressResultRecord result = Instancio.create(SubmitActivityProgressResultRecord.class);

		// When:
		ActivityProgressResponseV1 actualResult = lessonActivityApiMapperImpl.toActivityProgressResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToActivityProgressResponseV1SubmitActivityProgressResultRecordWithNullTest() {
		// Given:
		SubmitActivityProgressResultRecord result = null;

		// When:
		ActivityProgressResponseV1 actualResult = lessonActivityApiMapperImpl.toActivityProgressResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}

}