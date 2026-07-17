package com.aidigital.aionboarding.mappers.lesson;

import com.aidigital.aionboarding.api.v1.model.AddLessonAssetRequestV1;
import com.aidigital.aionboarding.api.v1.model.AskLessonResponseV1;
import com.aidigital.aionboarding.api.v1.model.ChatMessageV1;
import com.aidigital.aionboarding.api.v1.model.ChatRoleV1;
import com.aidigital.aionboarding.api.v1.model.CreateLessonRequestV1;
import com.aidigital.aionboarding.api.v1.model.LessonActivityV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssetResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssetV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssistantConversationResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonAssistantPresetV1;
import com.aidigital.aionboarding.api.v1.model.LessonContentFormatV1;
import com.aidigital.aionboarding.api.v1.model.LessonDetailResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonEnrollmentV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import com.aidigital.aionboarding.api.v1.model.LessonResponseV1;
import com.aidigital.aionboarding.api.v1.model.LessonRevisionKindV1;
import com.aidigital.aionboarding.api.v1.model.LessonRoadmapContextV1;
import com.aidigital.aionboarding.api.v1.model.LessonSummaryV1;
import com.aidigital.aionboarding.api.v1.model.LessonV1;
import com.aidigital.aionboarding.api.v1.model.LessonVisibilityV1;
import com.aidigital.aionboarding.api.v1.model.MaterialInputTypeV1;
import com.aidigital.aionboarding.api.v1.model.MaterialTypeV1;
import com.aidigital.aionboarding.api.v1.model.MaterialV1;
import com.aidigital.aionboarding.api.v1.model.ReviseLessonRequestV1;
import com.aidigital.aionboarding.api.v1.model.RevisionBriefV1;
import com.aidigital.aionboarding.api.v1.model.RevisionHistoryItemV1;
import com.aidigital.aionboarding.api.v1.model.TeacherVideoProviderV1;
import com.aidigital.aionboarding.api.v1.model.TeacherVideoResponseV1;
import com.aidigital.aionboarding.api.v1.model.TeacherVideoV1;
import com.aidigital.aionboarding.api.v1.model.UpdateLessonContentRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadedFileResponseV1;
import com.aidigital.aionboarding.mappers.common.ChatRoleApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonAssistantPresetApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonContentFormatApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonRevisionKindApiMapper;
import com.aidigital.aionboarding.mappers.common.LessonVisibilityApiMapper;
import com.aidigital.aionboarding.mappers.common.MaterialInputTypeApiMapper;
import com.aidigital.aionboarding.mappers.common.MaterialTypeApiMapper;
import com.aidigital.aionboarding.mappers.common.TeacherVideoProviderApiMapper;
import com.aidigital.aionboarding.mappers.learning.LessonEnrollmentApiMapper;
import com.aidigital.aionboarding.mappers.lessonactivity.LessonActivityApiMapper;
import com.aidigital.aionboarding.mappers.material.MaterialApiMapper;
import com.aidigital.aionboarding.service.learning.models.LessonEnrollmentRecord;
import com.aidigital.aionboarding.service.lesson.models.AskLessonResultRecord;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssistantConversationRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonRoadmapContextRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.ReviseLessonInput;
import com.aidigital.aionboarding.service.lesson.models.RevisionBriefRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionHistoryItemRecord;
import com.aidigital.aionboarding.service.lesson.models.RevisionResultRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoRecord;
import com.aidigital.aionboarding.service.lesson.models.TeacherVideoResultRecord;
import com.aidigital.aionboarding.service.lesson.models.UpdateLessonContentInput;
import com.aidigital.aionboarding.service.lessonactivity.models.LessonActivityRecord;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
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
class LessonApiMapperImplTest {

	@InjectMocks
	private LessonApiMapperImpl lessonApiMapperImpl;

	@Mock
	private MaterialTypeApiMapper materialTypeApiMapper;

	@Mock
	private MaterialInputTypeApiMapper materialInputTypeApiMapper;

	@Mock
	private LessonGenerationStatusApiMapper lessonGenerationStatusApiMapper;

	@Mock
	private LessonVisibilityApiMapper lessonVisibilityApiMapper;

	@Mock
	private LessonContentFormatApiMapper lessonContentFormatApiMapper;

	@Mock
	private LessonRevisionKindApiMapper lessonRevisionKindApiMapper;

	@Mock
	private TeacherVideoProviderApiMapper teacherVideoProviderApiMapper;

	@Mock
	private LessonActivityApiMapper lessonActivityApiMapper;

	@Mock
	private LessonEnrollmentApiMapper lessonEnrollmentApiMapper;

	@Mock
	private MaterialApiMapper materialApiMapper;

	@Mock
	private ChatRoleApiMapper chatRoleApiMapper;

	@Mock
	private LessonAssistantPresetApiMapper lessonAssistantPresetApiMapper;

	@BeforeEach
	void setUp() {
		when(materialTypeApiMapper.mapMaterialType(anyString())).thenReturn(Instancio.create(MaterialTypeV1.class));
		when(materialInputTypeApiMapper.fromMaterialInputType(any(MaterialInputTypeV1.class))).thenReturn("value");
		when(lessonGenerationStatusApiMapper.mapLessonGenerationStatus(anyString())).thenReturn(Instancio.create(LessonGenerationStatusV1.class));
		when(lessonVisibilityApiMapper.mapLessonVisibility(anyString())).thenReturn(Instancio.create(LessonVisibilityV1.class));
		when(lessonContentFormatApiMapper.mapLessonContentFormat(anyString())).thenReturn(Instancio.create(LessonContentFormatV1.class));
		when(lessonRevisionKindApiMapper.mapLessonRevisionKind(anyString())).thenReturn(Instancio.create(LessonRevisionKindV1.class));
		when(teacherVideoProviderApiMapper.mapTeacherVideoProvider(anyString())).thenReturn(Instancio.create(TeacherVideoProviderV1.class));
		when(lessonActivityApiMapper.toLessonActivityV1(any(LessonActivityRecord.class))).thenReturn(Instancio.create(LessonActivityV1.class));
		when(lessonEnrollmentApiMapper.toLessonEnrollmentV1(any(LessonEnrollmentRecord.class))).thenReturn(Instancio.create(LessonEnrollmentV1.class));
		when(materialApiMapper.toMaterialV1(any(MaterialRecord.class))).thenReturn(Instancio.create(MaterialV1.class));
		when(chatRoleApiMapper.mapChatRole(anyString())).thenReturn(Instancio.create(ChatRoleV1.class));
		when(lessonAssistantPresetApiMapper.fromAssistantPreset(anyString())).thenReturn(Instancio.create(LessonAssistantPresetV1.class));
	}

	@Test
	void shouldToLessonAssetV1LessonAssetRecordTest() {
		// Given:
		LessonAssetRecord asset = Instancio.create(LessonAssetRecord.class);

		// When:
		LessonAssetV1 actualResult = lessonApiMapperImpl.toLessonAssetV1(asset);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonAssetV1LessonAssetRecordWithNullTest() {
		// Given:
		LessonAssetRecord asset = null;

		// When:
		LessonAssetV1 actualResult = lessonApiMapperImpl.toLessonAssetV1(asset);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToChatMessageV1ChatTurnTest() {
		// Given:
		ChatTurn turn = Instancio.create(ChatTurn.class);

		// When:
		ChatMessageV1 actualResult = lessonApiMapperImpl.toChatMessageV1(turn);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToChatMessageV1ChatTurnWithNullTest() {
		// Given:
		ChatTurn turn = null;

		// When:
		ChatMessageV1 actualResult = lessonApiMapperImpl.toChatMessageV1(turn);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonAssistantConversationResponseV1LessonAssistantConversationRecordTest() {
		// Given:
		LessonAssistantConversationRecord conversation = Instancio.create(LessonAssistantConversationRecord.class);

		// When:
		LessonAssistantConversationResponseV1 actualResult =
				lessonApiMapperImpl.toLessonAssistantConversationResponseV1(conversation);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonAssistantConversationResponseV1LessonAssistantConversationRecordWithNullTest() {
		// Given:
		LessonAssistantConversationRecord conversation = null;

		// When:
		LessonAssistantConversationResponseV1 actualResult =
				lessonApiMapperImpl.toLessonAssistantConversationResponseV1(conversation);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonSummaryV1LessonSearchSummaryRecordTest() {
		// Given:
		LessonSearchSummaryRecord lesson = Instancio.create(LessonSearchSummaryRecord.class);

		// When:
		LessonSummaryV1 actualResult = lessonApiMapperImpl.toLessonSummaryV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonSummaryV1LessonSearchSummaryRecordWithNullTest() {
		// Given:
		LessonSearchSummaryRecord lesson = null;

		// When:
		LessonSummaryV1 actualResult = lessonApiMapperImpl.toLessonSummaryV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonGenerationStatusResponseV1StringTest() {
		// Given:
		String statusCode = "value";

		// When:
		LessonGenerationStatusResponseV1 actualResult =
				lessonApiMapperImpl.toLessonGenerationStatusResponseV1(statusCode);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonGenerationStatusResponseV1StringWithNullTest() {
		// Given:
		String statusCode = null;

		// When:
		LessonGenerationStatusResponseV1 actualResult =
				lessonApiMapperImpl.toLessonGenerationStatusResponseV1(statusCode);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRevisionHistoryItemV1RevisionHistoryItemRecordTest() {
		// Given:
		RevisionHistoryItemRecord item = Instancio.create(RevisionHistoryItemRecord.class);

		// When:
		RevisionHistoryItemV1 actualResult = lessonApiMapperImpl.toRevisionHistoryItemV1(item);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRevisionHistoryItemV1RevisionHistoryItemRecordWithNullTest() {
		// Given:
		RevisionHistoryItemRecord item = null;

		// When:
		RevisionHistoryItemV1 actualResult = lessonApiMapperImpl.toRevisionHistoryItemV1(item);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRevisionBriefV1RevisionBriefRecordTest() {
		// Given:
		RevisionBriefRecord brief = Instancio.create(RevisionBriefRecord.class);

		// When:
		RevisionBriefV1 actualResult = lessonApiMapperImpl.toRevisionBriefV1(brief);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRevisionBriefV1RevisionBriefRecordWithNullTest() {
		// Given:
		RevisionBriefRecord brief = null;

		// When:
		RevisionBriefV1 actualResult = lessonApiMapperImpl.toRevisionBriefV1(brief);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonV1LessonDetailRecordTest() {
		// Given:
		LessonDetailRecord lesson = Instancio.create(LessonDetailRecord.class);

		// When:
		LessonV1 actualResult = lessonApiMapperImpl.toLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonV1LessonDetailRecordWithNullTest() {
		// Given:
		LessonDetailRecord lesson = null;

		// When:
		LessonV1 actualResult = lessonApiMapperImpl.toLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonResponseV1LessonSummaryRecordTest() {
		// Given:
		LessonSummaryRecord lesson = Instancio.create(LessonSummaryRecord.class);

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonResponseV1LessonSummaryRecordWithNullTest() {
		// Given:
		LessonSummaryRecord lesson = null;

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonResponseV1LessonDetailRecordTest() {
		// Given:
		LessonDetailRecord lesson = Instancio.create(LessonDetailRecord.class);

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonResponseV1LessonDetailRecordWithNullTest() {
		// Given:
		LessonDetailRecord lesson = null;

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonResponseV1RevisionResultRecordTest() {
		// Given:
		RevisionResultRecord result = Instancio.create(RevisionResultRecord.class);

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonResponseV1RevisionResultRecordWithNullTest() {
		// Given:
		RevisionResultRecord result = null;

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonDetailResponseV1LessonDetailResultRecordTest() {
		// Given:
		LessonDetailResultRecord result = Instancio.create(LessonDetailResultRecord.class);

		// When:
		LessonDetailResponseV1 actualResult = lessonApiMapperImpl.toLessonDetailResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonDetailResponseV1LessonDetailResultRecordWithNullTest() {
		// Given:
		LessonDetailResultRecord result = null;

		// When:
		LessonDetailResponseV1 actualResult = lessonApiMapperImpl.toLessonDetailResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapContextV1LessonRoadmapContextRecordTest() {
		// Given:
		LessonRoadmapContextRecord record = Instancio.create(LessonRoadmapContextRecord.class);

		// When:
		LessonRoadmapContextV1 actualResult = lessonApiMapperImpl.toRoadmapContextV1(record);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapContextV1LessonRoadmapContextRecordWithNullTest() {
		// Given:
		LessonRoadmapContextRecord record = null;

		// When:
		LessonRoadmapContextV1 actualResult = lessonApiMapperImpl.toRoadmapContextV1(record);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToCreateLessonInputCreateLessonRequestV1Test() {
		// Given:
		CreateLessonRequestV1 request = Instancio.create(CreateLessonRequestV1.class);

		// When:
		CreateLessonInput actualResult = lessonApiMapperImpl.toCreateLessonInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCreateLessonInputCreateLessonRequestV1WithNullTest() {
		// Given:
		CreateLessonRequestV1 request = null;

		// When:
		CreateLessonInput actualResult = lessonApiMapperImpl.toCreateLessonInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToUpdateLessonContentInputUpdateLessonContentRequestV1Test() {
		// Given:
		UpdateLessonContentRequestV1 request = Instancio.create(UpdateLessonContentRequestV1.class);

		// When:
		UpdateLessonContentInput actualResult = lessonApiMapperImpl.toUpdateLessonContentInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUpdateLessonContentInputUpdateLessonContentRequestV1WithNullTest() {
		// Given:
		UpdateLessonContentRequestV1 request = null;

		// When:
		UpdateLessonContentInput actualResult = lessonApiMapperImpl.toUpdateLessonContentInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToReviseLessonInputReviseLessonRequestV1Test() {
		// Given:
		ReviseLessonRequestV1 request = Instancio.create(ReviseLessonRequestV1.class);

		// When:
		ReviseLessonInput actualResult = lessonApiMapperImpl.toReviseLessonInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToReviseLessonInputReviseLessonRequestV1WithNullTest() {
		// Given:
		ReviseLessonRequestV1 request = null;

		// When:
		ReviseLessonInput actualResult = lessonApiMapperImpl.toReviseLessonInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToCreateLessonAssetInputAddLessonAssetRequestV1Test() {
		// Given:
		AddLessonAssetRequestV1 request = Instancio.create(AddLessonAssetRequestV1.class);

		// When:
		CreateLessonAssetInput actualResult = lessonApiMapperImpl.toCreateLessonAssetInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCreateLessonAssetInputAddLessonAssetRequestV1WithNullTest() {
		// Given:
		AddLessonAssetRequestV1 request = null;

		// When:
		CreateLessonAssetInput actualResult = lessonApiMapperImpl.toCreateLessonAssetInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToAskLessonResponseV1AskLessonResultRecordTest() {
		// Given:
		AskLessonResultRecord result = Instancio.create(AskLessonResultRecord.class);

		// When:
		AskLessonResponseV1 actualResult = lessonApiMapperImpl.toAskLessonResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToAskLessonResponseV1AskLessonResultRecordWithNullTest() {
		// Given:
		AskLessonResultRecord result = null;

		// When:
		AskLessonResponseV1 actualResult = lessonApiMapperImpl.toAskLessonResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonAssetResponseV1LessonAssetResultRecordTest() {
		// Given:
		LessonAssetResultRecord result = Instancio.create(LessonAssetResultRecord.class);

		// When:
		LessonAssetResponseV1 actualResult = lessonApiMapperImpl.toLessonAssetResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonAssetResponseV1LessonAssetResultRecordWithNullTest() {
		// Given:
		LessonAssetResultRecord result = null;

		// When:
		LessonAssetResponseV1 actualResult = lessonApiMapperImpl.toLessonAssetResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToLessonResponseV1LessonAssetDeleteResultRecordTest() {
		// Given:
		LessonAssetDeleteResultRecord result = Instancio.create(LessonAssetDeleteResultRecord.class);

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonResponseV1LessonAssetDeleteResultRecordWithNullTest() {
		// Given:
		LessonAssetDeleteResultRecord result = null;

		// When:
		LessonResponseV1 actualResult = lessonApiMapperImpl.toLessonResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeacherVideoV1TeacherVideoRecordTest() {
		// Given:
		TeacherVideoRecord teacherVideo = Instancio.create(TeacherVideoRecord.class);

		// When:
		TeacherVideoV1 actualResult = lessonApiMapperImpl.toTeacherVideoV1(teacherVideo);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeacherVideoV1TeacherVideoRecordWithNullTest() {
		// Given:
		TeacherVideoRecord teacherVideo = null;

		// When:
		TeacherVideoV1 actualResult = lessonApiMapperImpl.toTeacherVideoV1(teacherVideo);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToTeacherVideoResponseV1TeacherVideoResultRecordTest() {
		// Given:
		TeacherVideoResultRecord result = Instancio.create(TeacherVideoResultRecord.class);

		// When:
		TeacherVideoResponseV1 actualResult = lessonApiMapperImpl.toTeacherVideoResponseV1(result);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToTeacherVideoResponseV1TeacherVideoResultRecordWithNullTest() {
		// Given:
		TeacherVideoResultRecord result = null;

		// When:
		TeacherVideoResponseV1 actualResult = lessonApiMapperImpl.toTeacherVideoResponseV1(result);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToUploadedFileResponseV1StringStringStringlongTest() {
		// Given:
		String storageKey = "value";
		String originalName = "value";
		String mimeType = "value";
		long sizeBytes = 5L;

		// When:
		UploadedFileResponseV1 actualResult = lessonApiMapperImpl.toUploadedFileResponseV1(storageKey, originalName,
				mimeType, sizeBytes);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonV1LessonSummaryRecordTest() {
		// Given:
		LessonSummaryRecord lesson = Instancio.create(LessonSummaryRecord.class);

		// When:
		LessonV1 actualResult = lessonApiMapperImpl.toLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToLessonV1LessonSummaryRecordWithNullTest() {
		// Given:
		LessonSummaryRecord lesson = null;

		// When:
		LessonV1 actualResult = lessonApiMapperImpl.toLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}

}