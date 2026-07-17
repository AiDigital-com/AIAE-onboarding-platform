package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.ActivityCompletionModeV1;
import com.aidigital.aionboarding.api.v1.model.ChatRoleV1;
import com.aidigital.aionboarding.api.v1.model.DashboardPeriodV1;
import com.aidigital.aionboarding.api.v1.model.LearningStatusV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityTypeV1;
import com.aidigital.aionboarding.api.v1.model.LessonContentFormatV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import com.aidigital.aionboarding.api.v1.model.LessonRevisionKindV1;
import com.aidigital.aionboarding.api.v1.model.LessonVisibilityV1;
import com.aidigital.aionboarding.api.v1.model.MaterialAssetKindV1;
import com.aidigital.aionboarding.api.v1.model.MaterialInputTypeV1;
import com.aidigital.aionboarding.api.v1.model.MaterialTypeV1;
import com.aidigital.aionboarding.api.v1.model.PreparationStatusV1;
import com.aidigital.aionboarding.api.v1.model.ProgressStatusV1;
import com.aidigital.aionboarding.api.v1.model.QuizQuestionTypeV1;
import com.aidigital.aionboarding.api.v1.model.TeacherVideoProviderV1;
import com.aidigital.aionboarding.api.v1.model.UserRoleCodeV1;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonEnumApiMapperEdgeCasesTest {

	private final ActivityCompletionModeApiMapper activityCompletionModeApiMapper = new ActivityCompletionModeApiMapperImpl();
	private final ChatRoleApiMapper chatRoleApiMapper = new ChatRoleApiMapperImpl();
	private final DashboardPeriodApiMapper dashboardPeriodApiMapper = new DashboardPeriodApiMapperImpl();
	private final LearningStatusApiMapper learningStatusApiMapper = new LearningStatusApiMapperImpl();
	private final LessonActivityTypeApiMapper lessonActivityTypeApiMapper = new LessonActivityTypeApiMapperImpl();
	private final LessonContentFormatApiMapper lessonContentFormatApiMapper = new LessonContentFormatApiMapperImpl();
	private final LessonGenerationStatusApiMapper lessonGenerationStatusApiMapper = new LessonGenerationStatusApiMapperImpl();
	private final LessonRevisionKindApiMapper lessonRevisionKindApiMapper = new LessonRevisionKindApiMapperImpl();
	private final LessonVisibilityApiMapper lessonVisibilityApiMapper = new LessonVisibilityApiMapperImpl();
	private final MaterialAssetKindApiMapper materialAssetKindApiMapper = new MaterialAssetKindApiMapperImpl();
	private final MaterialInputTypeApiMapper materialInputTypeApiMapper = new MaterialInputTypeApiMapperImpl();
	private final MaterialTypeApiMapper materialTypeApiMapper = new MaterialTypeApiMapperImpl();
	private final PreparationStatusApiMapper preparationStatusApiMapper = new PreparationStatusApiMapperImpl();
	private final ProgressStatusApiMapper progressStatusApiMapper = new ProgressStatusApiMapperImpl();
	private final QuizQuestionTypeApiMapper quizQuestionTypeApiMapper = new QuizQuestionTypeApiMapperImpl();
	private final TeacherVideoProviderApiMapper teacherVideoProviderApiMapper = new TeacherVideoProviderApiMapperImpl();
	private final UserRoleCodeApiMapper userRoleCodeApiMapper = new UserRoleCodeApiMapperImpl();

	@Test
	void shouldReturnDefaultForNullOrBlankOrInvalidStringTest() {
		// Given / When / Then:
		assertThat(activityCompletionModeApiMapper.mapActivityCompletionMode(null)).isEqualTo(ActivityCompletionModeV1.FINISHED);
		assertThat(activityCompletionModeApiMapper.mapActivityCompletionMode(" ")).isEqualTo(ActivityCompletionModeV1.FINISHED);
		assertThat(activityCompletionModeApiMapper.mapActivityCompletionMode("unknown")).isEqualTo(ActivityCompletionModeV1.FINISHED);

		assertThat(chatRoleApiMapper.mapChatRole(null)).isEqualTo(ChatRoleV1.USER);
		assertThat(chatRoleApiMapper.mapChatRole("")).isEqualTo(ChatRoleV1.USER);
		assertThat(chatRoleApiMapper.mapChatRole("invalid")).isEqualTo(ChatRoleV1.USER);

		assertThat(dashboardPeriodApiMapper.mapDashboardPeriod(null)).isEqualTo(DashboardPeriodV1.MONTH);
		assertThat(dashboardPeriodApiMapper.mapDashboardPeriod("  ")).isEqualTo(DashboardPeriodV1.MONTH);
		assertThat(dashboardPeriodApiMapper.mapDashboardPeriod("invalid")).isEqualTo(DashboardPeriodV1.MONTH);

		assertThat(learningStatusApiMapper.mapLearningStatus(null)).isEqualTo(LearningStatusV1.IN_PROGRESS);
		assertThat(learningStatusApiMapper.mapLearningStatus("")).isEqualTo(LearningStatusV1.IN_PROGRESS);
		assertThat(learningStatusApiMapper.mapLearningStatus("invalid")).isEqualTo(LearningStatusV1.IN_PROGRESS);

		assertThat(lessonActivityTypeApiMapper.mapLessonActivityType(null)).isEqualTo(LessonActivityTypeV1.QUIZ);
		assertThat(lessonActivityTypeApiMapper.mapLessonActivityType(" ")).isEqualTo(LessonActivityTypeV1.QUIZ);
		assertThat(lessonActivityTypeApiMapper.mapLessonActivityType("invalid")).isEqualTo(LessonActivityTypeV1.QUIZ);

		assertThat(lessonContentFormatApiMapper.mapLessonContentFormat(null)).isEqualTo(LessonContentFormatV1.MARKDOWN);
		assertThat(lessonContentFormatApiMapper.mapLessonContentFormat("")).isEqualTo(LessonContentFormatV1.MARKDOWN);
		assertThat(lessonContentFormatApiMapper.mapLessonContentFormat("invalid")).isEqualTo(LessonContentFormatV1.MARKDOWN);

		assertThat(lessonGenerationStatusApiMapper.mapLessonGenerationStatus(null)).isEqualTo(LessonGenerationStatusV1.DRAFT);
		assertThat(lessonGenerationStatusApiMapper.mapLessonGenerationStatus(" ")).isEqualTo(LessonGenerationStatusV1.DRAFT);
		assertThat(lessonGenerationStatusApiMapper.mapLessonGenerationStatus("invalid")).isEqualTo(LessonGenerationStatusV1.DRAFT);

		assertThat(lessonRevisionKindApiMapper.mapLessonRevisionKind(null)).isEqualTo(LessonRevisionKindV1.SUBSTANTIAL);
		assertThat(lessonRevisionKindApiMapper.mapLessonRevisionKind("")).isEqualTo(LessonRevisionKindV1.SUBSTANTIAL);
		assertThat(lessonRevisionKindApiMapper.mapLessonRevisionKind("invalid")).isEqualTo(LessonRevisionKindV1.SUBSTANTIAL);

		assertThat(lessonVisibilityApiMapper.mapLessonVisibility(null)).isEqualTo(LessonVisibilityV1.PUBLISHED);
		assertThat(lessonVisibilityApiMapper.mapLessonVisibility(" ")).isEqualTo(LessonVisibilityV1.PUBLISHED);
		assertThat(lessonVisibilityApiMapper.mapLessonVisibility("invalid")).isEqualTo(LessonVisibilityV1.PUBLISHED);

		assertThat(materialAssetKindApiMapper.mapMaterialAssetKind(null)).isEqualTo(MaterialAssetKindV1.FILE);
		assertThat(materialAssetKindApiMapper.mapMaterialAssetKind(" ")).isEqualTo(MaterialAssetKindV1.FILE);
		assertThat(materialAssetKindApiMapper.mapMaterialAssetKind("invalid")).isEqualTo(MaterialAssetKindV1.FILE);

		assertThat(materialInputTypeApiMapper.mapMaterialInputType(null)).isEqualTo(MaterialInputTypeV1.LINK);
		assertThat(materialInputTypeApiMapper.mapMaterialInputType("")).isEqualTo(MaterialInputTypeV1.LINK);
		assertThat(materialInputTypeApiMapper.mapMaterialInputType("invalid")).isEqualTo(MaterialInputTypeV1.LINK);

		assertThat(materialTypeApiMapper.mapMaterialType(null)).isEqualTo(MaterialTypeV1.LINK);
		assertThat(materialTypeApiMapper.mapMaterialType(" ")).isEqualTo(MaterialTypeV1.LINK);
		assertThat(materialTypeApiMapper.mapMaterialType("invalid")).isEqualTo(MaterialTypeV1.LINK);

		assertThat(preparationStatusApiMapper.mapPreparationStatus(null)).isEqualTo(PreparationStatusV1.NOT_STARTED);
		assertThat(preparationStatusApiMapper.mapPreparationStatus("")).isEqualTo(PreparationStatusV1.NOT_STARTED);
		assertThat(preparationStatusApiMapper.mapPreparationStatus("invalid")).isEqualTo(PreparationStatusV1.NOT_STARTED);

		assertThat(progressStatusApiMapper.mapProgressStatus(null)).isEqualTo(ProgressStatusV1.NOT_STARTED);
		assertThat(progressStatusApiMapper.mapProgressStatus(" ")).isEqualTo(ProgressStatusV1.NOT_STARTED);
		assertThat(progressStatusApiMapper.mapProgressStatus("invalid")).isEqualTo(ProgressStatusV1.NOT_STARTED);

		assertThat(quizQuestionTypeApiMapper.mapQuizQuestionType(null)).isEqualTo(QuizQuestionTypeV1.MULTIPLE_CHOICE);
		assertThat(quizQuestionTypeApiMapper.mapQuizQuestionType("")).isEqualTo(QuizQuestionTypeV1.MULTIPLE_CHOICE);
		assertThat(quizQuestionTypeApiMapper.mapQuizQuestionType("invalid")).isEqualTo(QuizQuestionTypeV1.MULTIPLE_CHOICE);

		assertThat(teacherVideoProviderApiMapper.mapTeacherVideoProvider(null)).isEqualTo(TeacherVideoProviderV1.HEYGEN);
		assertThat(teacherVideoProviderApiMapper.mapTeacherVideoProvider(" ")).isEqualTo(TeacherVideoProviderV1.HEYGEN);
		assertThat(teacherVideoProviderApiMapper.mapTeacherVideoProvider("invalid")).isEqualTo(TeacherVideoProviderV1.HEYGEN);

		assertThat(userRoleCodeApiMapper.mapUserRoleCode(null)).isEqualTo(UserRoleCodeV1.MEMBER);
		assertThat(userRoleCodeApiMapper.mapUserRoleCode("")).isEqualTo(UserRoleCodeV1.MEMBER);
		assertThat(userRoleCodeApiMapper.mapUserRoleCode("invalid")).isEqualTo(UserRoleCodeV1.MEMBER);
	}

	@Test
	void shouldMapValidStringValuesTest() {
		// Given / When / Then:
		assertThat(activityCompletionModeApiMapper.mapActivityCompletionMode("scored")).isEqualTo(ActivityCompletionModeV1.SCORED);
		assertThat(activityCompletionModeApiMapper.mapActivityCompletionMode("finished")).isEqualTo(ActivityCompletionModeV1.FINISHED);

		assertThat(chatRoleApiMapper.mapChatRole("assistant")).isEqualTo(ChatRoleV1.ASSISTANT);
		assertThat(chatRoleApiMapper.mapChatRole("user")).isEqualTo(ChatRoleV1.USER);

		assertThat(dashboardPeriodApiMapper.mapDashboardPeriod("week")).isEqualTo(DashboardPeriodV1.WEEK);
		assertThat(dashboardPeriodApiMapper.mapDashboardPeriod("month")).isEqualTo(DashboardPeriodV1.MONTH);

		assertThat(learningStatusApiMapper.mapLearningStatus("in-progress")).isEqualTo(LearningStatusV1.IN_PROGRESS);
		assertThat(learningStatusApiMapper.mapLearningStatus("completed")).isEqualTo(LearningStatusV1.COMPLETED);

		assertThat(lessonActivityTypeApiMapper.mapLessonActivityType("quiz")).isEqualTo(LessonActivityTypeV1.QUIZ);
		assertThat(lessonActivityTypeApiMapper.mapLessonActivityType("flashcards")).isEqualTo(LessonActivityTypeV1.FLASHCARDS);

		assertThat(lessonContentFormatApiMapper.mapLessonContentFormat("markdown")).isEqualTo(LessonContentFormatV1.MARKDOWN);

		assertThat(lessonGenerationStatusApiMapper.mapLessonGenerationStatus("ready")).isEqualTo(LessonGenerationStatusV1.READY);
		assertThat(lessonGenerationStatusApiMapper.mapLessonGenerationStatus("draft")).isEqualTo(LessonGenerationStatusV1.DRAFT);

		assertThat(lessonRevisionKindApiMapper.mapLessonRevisionKind("targeted")).isEqualTo(LessonRevisionKindV1.TARGETED);
		assertThat(lessonRevisionKindApiMapper.mapLessonRevisionKind("substantial")).isEqualTo(LessonRevisionKindV1.SUBSTANTIAL);

		assertThat(lessonVisibilityApiMapper.mapLessonVisibility("published")).isEqualTo(LessonVisibilityV1.PUBLISHED);
		assertThat(lessonVisibilityApiMapper.mapLessonVisibility("private")).isEqualTo(LessonVisibilityV1.PRIVATE);

		assertThat(materialAssetKindApiMapper.mapMaterialAssetKind("file")).isEqualTo(MaterialAssetKindV1.FILE);
		assertThat(materialAssetKindApiMapper.mapMaterialAssetKind("image")).isEqualTo(MaterialAssetKindV1.IMAGE);

		assertThat(materialInputTypeApiMapper.mapMaterialInputType("youtube")).isEqualTo(MaterialInputTypeV1.YOUTUBE);
		assertThat(materialInputTypeApiMapper.mapMaterialInputType("link")).isEqualTo(MaterialInputTypeV1.LINK);

		assertThat(materialTypeApiMapper.mapMaterialType("file")).isEqualTo(MaterialTypeV1.FILE);
		assertThat(materialTypeApiMapper.mapMaterialType("link")).isEqualTo(MaterialTypeV1.LINK);

		assertThat(preparationStatusApiMapper.mapPreparationStatus("in_progress")).isEqualTo(PreparationStatusV1.IN_PROGRESS);
		assertThat(preparationStatusApiMapper.mapPreparationStatus("not_started")).isEqualTo(PreparationStatusV1.NOT_STARTED);

		assertThat(progressStatusApiMapper.mapProgressStatus("in-progress")).isEqualTo(ProgressStatusV1.IN_PROGRESS);
		assertThat(progressStatusApiMapper.mapProgressStatus("not-started")).isEqualTo(ProgressStatusV1.NOT_STARTED);

		assertThat(quizQuestionTypeApiMapper.mapQuizQuestionType("single_choice")).isEqualTo(QuizQuestionTypeV1.SINGLE_CHOICE);
		assertThat(quizQuestionTypeApiMapper.mapQuizQuestionType("multiple_choice")).isEqualTo(QuizQuestionTypeV1.MULTIPLE_CHOICE);

		assertThat(teacherVideoProviderApiMapper.mapTeacherVideoProvider("heygen")).isEqualTo(TeacherVideoProviderV1.HEYGEN);

		assertThat(userRoleCodeApiMapper.mapUserRoleCode("admin")).isEqualTo(UserRoleCodeV1.ADMIN);
		assertThat(userRoleCodeApiMapper.mapUserRoleCode("member")).isEqualTo(UserRoleCodeV1.MEMBER);
	}

	@Test
	void shouldReturnNullForNullEnumTest() {
		// Given / When / Then:
		assertThat(activityCompletionModeApiMapper.fromActivityCompletionMode(null)).isNull();
		assertThat(chatRoleApiMapper.fromChatRole(null)).isNull();
		assertThat(dashboardPeriodApiMapper.fromDashboardPeriod(null)).isNull();
		assertThat(learningStatusApiMapper.fromLearningStatus(null)).isNull();
		assertThat(lessonActivityTypeApiMapper.fromLessonActivityType(null)).isNull();
		assertThat(lessonContentFormatApiMapper.fromLessonContentFormat(null)).isNull();
		assertThat(lessonGenerationStatusApiMapper.fromLessonGenerationStatus(null)).isNull();
		assertThat(lessonRevisionKindApiMapper.fromLessonRevisionKind(null)).isNull();
		assertThat(lessonVisibilityApiMapper.fromLessonVisibility(null)).isNull();
		assertThat(materialAssetKindApiMapper.fromMaterialAssetKind(null)).isNull();
		assertThat(materialInputTypeApiMapper.fromMaterialInputType(null)).isNull();
		assertThat(materialTypeApiMapper.fromMaterialType(null)).isNull();
		assertThat(preparationStatusApiMapper.fromPreparationStatus(null)).isNull();
		assertThat(progressStatusApiMapper.fromProgressStatus(null)).isNull();
		assertThat(quizQuestionTypeApiMapper.fromQuizQuestionType(null)).isNull();
		assertThat(teacherVideoProviderApiMapper.fromTeacherVideoProvider(null)).isNull();
		assertThat(userRoleCodeApiMapper.fromUserRoleCode(null)).isNull();
	}
}
