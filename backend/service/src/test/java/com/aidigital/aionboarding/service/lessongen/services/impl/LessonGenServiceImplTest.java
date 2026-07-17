package com.aidigital.aionboarding.service.lessongen.services.impl;

import com.aidigital.aionboarding.external.openai.OpenAiClient;
import com.aidigital.aionboarding.external.openai.OpenAiExternalException;
import com.aidigital.aionboarding.external.openai.config.OpenAiProperties;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileInput;
import com.aidigital.aionboarding.external.openai.model.OpenAiFileUploadResponse;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesResult;
import com.aidigital.aionboarding.external.openai.model.OpenAiResponsesUsage;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.lessongen.config.LessonGenProperties;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedActivityResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.GeneratedRevisionBriefResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.support.GenerationMetadataAssembler;
import com.aidigital.aionboarding.service.lessongen.util.LessonGenJsonSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonGenServiceImplTest {

	@Mock
	private ObjectProvider<OpenAiClient> openAiClientProvider;
	@Mock
	private OpenAiClient openAiClient;
	@Mock
	private OpenAiProperties openAiProperties;
	@Mock
	private LessonGenJsonSupport lessonGenJsonSupport;
	private final GenerationMetadataAssembler generationMetadataAssembler = new GenerationMetadataAssembler();

	private LessonGenServiceImpl service(LessonGenProperties lessonGenProperties) {
		return new LessonGenServiceImpl(
				openAiClientProvider, openAiProperties, lessonGenProperties, lessonGenJsonSupport,
				generationMetadataAssembler);
	}

	private LessonGenProperties defaultProperties() {
		return new LessonGenProperties();
	}

	@Test
	void shouldUseGpt4oMiniByDefaultForGenerateLessonContentTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions", "input");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-1", new OpenAiResponsesUsage(1, 2, 3), "generated content");
		when(openAiClient.createResponse(eq("instructions"), eq("input"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);

		// When:
		GeneratedContentResult generated = service.generateLessonContent(prompt);

		// Then:
		assertThat(generated.content()).isEqualTo("generated content");
		assertThat(generated.metadata().get("model")).isEqualTo("gpt-4o-mini");
	}

	@Test
	void shouldUseConfiguredModelOverrideForGenerateLessonContentTest() {
		// Given:
		LessonGenProperties lessonGenProperties = defaultProperties();
		lessonGenProperties.setModel("gpt-4o");
		LessonGenServiceImpl service = service(lessonGenProperties);
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions", "input");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-2", new OpenAiResponsesUsage(4, 5, 9), "overridden content");
		ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);
		when(openAiClient.createResponse(eq("instructions"), eq("input"), modelCaptor.capture(),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);

		// When:
		GeneratedContentResult generated = service.generateLessonContent(prompt);

		// Then:
		assertThat(generated.content()).isEqualTo("overridden content");
		assertThat(modelCaptor.getValue()).isEqualTo("gpt-4o");
		assertThat(generated.metadata().get("model")).isEqualTo("gpt-4o");
	}

	@Test
	void shouldForwardFileInputsToOpenAiClientTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		List<Map<String, Object>> rawFileInputs = List.of(
				Map.of("type", "input_file", "file_id", "file-abc")
		);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions", "input", rawFileInputs);
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-3", new OpenAiResponsesUsage(1, 1, 2), "file-based content");
		ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List<OpenAiFileInput>> fileInputCaptor = ArgumentCaptor.forClass(List.class);
		when(openAiClient.createResponse(eq("instructions"), eq("input"), eq("gpt-4o-mini"),
				cacheKeyCaptor.capture(), fileInputCaptor.capture(), isNull(), isNull()))
				.thenReturn(result);

		// When:
		GeneratedContentResult generated = service.generateLessonContent(prompt);

		// Then:
		assertThat(generated.content()).isEqualTo("file-based content");
		assertThat(cacheKeyCaptor.getValue()).isEqualTo("cache-key");
		List<OpenAiFileInput> capturedFileInputs = fileInputCaptor.getValue();
		assertThat(capturedFileInputs).hasSize(1);
		assertThat(capturedFileInputs.get(0).type()).isEqualTo("input_file");
		assertThat(capturedFileInputs.get(0).fileId()).isEqualTo("file-abc");
	}

	@Test
	void shouldForwardStoreFlagAndPreviousResponseIdToOpenAiClientTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt(
				"v1", "cache-key", "instructions", "input", List.of(), true, "resp-previous");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-4", new OpenAiResponsesUsage(1, 1, 2), "chained content");
		when(openAiClient.createResponse(eq("instructions"), eq("input"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), eq(Boolean.TRUE), eq("resp-previous")))
				.thenReturn(result);

		// When:
		GeneratedContentResult generated = service.generateLessonContent(prompt);

		// Then:
		assertThat(generated.content()).isEqualTo("chained content");
	}

	@Test
	void shouldUseDefaultModelForCondenseSourceTextWhenNoOverrideTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "condense", "source text");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-5", new OpenAiResponsesUsage(1, 2, 3), "condensed");
		when(openAiClient.createResponse(eq("condense"), eq("source text"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);

		// When:
		GeneratedContentResult generated = service.condenseSourceText(prompt);

		// Then:
		assertThat(generated.content()).isEqualTo("condensed");
		assertThat(generated.metadata().get("model")).isEqualTo("gpt-4o-mini");
	}

	@Test
	void shouldPreferTranscriptCompressionModelForCondenseSourceTextTest() {
		// Given:
		LessonGenProperties properties = defaultProperties();
		properties.setTranscriptCompressionModel("gpt-4o");
		LessonGenServiceImpl service = service(properties);
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "condense", "source text");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-6", new OpenAiResponsesUsage(1, 2, 3), "condensed");
		ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);
		when(openAiClient.createResponse(eq("condense"), eq("source text"), modelCaptor.capture(),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);

		// When:
		GeneratedContentResult generated = service.condenseSourceText(prompt);

		// Then:
		assertThat(generated.content()).isEqualTo("condensed");
		assertThat(modelCaptor.getValue()).isEqualTo("gpt-4o");
		assertThat(generated.metadata().get("model")).isEqualTo("gpt-4o");
	}

	@Test
	void shouldThrowWhenCondensedTextIsBlankTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "condense", "source text");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-7", new OpenAiResponsesUsage(1, 2, 3), "  ");
		when(openAiClient.createResponse(eq("condense"), eq("source text"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);

		// When-Then:
		assertThatThrownBy(() -> service.condenseSourceText(prompt))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("empty condensed transcript");
	}

	@Test
	void shouldGenerateRevisionBriefWithDefaultsForInvalidScopeAndBlankIntentTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "plan", "feedback");
		String raw = "{\"changeScope\":\"huge\",\"userIntent\":\" \",\"editInstructions\":[\"a\"],\"preserveRules\":[],\"riskNotes\":[\" \",\"note\"]}";
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-8", new OpenAiResponsesUsage(1, 2, 3), raw);
		when(openAiClient.createResponse(eq("plan"), eq("feedback"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);
		when(lessonGenJsonSupport.extractJsonPayload(raw)).thenReturn(
				Map.of("changeScope", "huge", "userIntent", " ", "editInstructions", List.of("a"),
						"preserveRules", List.of(), "riskNotes", List.of(" ", "note")));

		// When:
		GeneratedRevisionBriefResult generated = service.generateLessonRevisionBrief(prompt);

		// Then:
		assertThat(generated.brief().get("changeScope")).isEqualTo("substantial");
		assertThat(generated.brief().get("userIntent")).isEqualTo(
				"Revise the current lesson based on user feedback.");
		@SuppressWarnings("unchecked")
		List<String> editInstructions = (List<String>) generated.brief().get("editInstructions");
		assertThat(editInstructions).containsExactly("a");
		@SuppressWarnings("unchecked")
		List<String> riskNotes = (List<String>) generated.brief().get("riskNotes");
		assertThat(riskNotes).containsExactly("note");
		assertThat(generated.metadata().get("rawOutput")).isEqualTo(raw);
	}

	@Test
	void shouldGenerateRevisionBriefWithValidScopeAndIntentTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "plan", "feedback");
		String raw = "{\"changeScope\":\"targeted\",\"userIntent\":\"Fix typos\",\"editInstructions\":[],\"preserveRules\":[],\"riskNotes\":[]}";
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-9", new OpenAiResponsesUsage(1, 2, 3), raw);
		when(openAiClient.createResponse(eq("plan"), eq("feedback"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);
		when(lessonGenJsonSupport.extractJsonPayload(raw)).thenReturn(
				Map.of("changeScope", "targeted", "userIntent", "Fix typos", "editInstructions", List.of(),
						"preserveRules", List.of(), "riskNotes", List.of()));

		// When:
		GeneratedRevisionBriefResult generated = service.generateLessonRevisionBrief(prompt);

		// Then:
		assertThat(generated.brief().get("changeScope")).isEqualTo("targeted");
		assertThat(generated.brief().get("userIntent")).isEqualTo("Fix typos");
	}

	@Test
	void shouldThrowWhenRevisionBriefJsonIsInvalidTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "plan", "feedback");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-10", new OpenAiResponsesUsage(1, 2, 3), "not-json");
		when(openAiClient.createResponse(eq("plan"), eq("feedback"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);
		when(lessonGenJsonSupport.extractJsonPayload("not-json")).thenReturn(null);

		// When-Then:
		assertThatThrownBy(() -> service.generateLessonRevisionBrief(prompt))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("invalid revision brief");
	}

	@Test
	void shouldPreferActivityModelThenMiniThenOpenAiModelForActivityPayloadTest() {
		// Given:
		LessonGenProperties properties = new LessonGenProperties();
		properties.setActivityModel(null);
		properties.setMiniModel(null);
		LessonGenServiceImpl service = service(properties);
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		when(openAiProperties.getModel()).thenReturn("gpt-4o");
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "activity", "lesson");
		String raw = "{\"items\":[]}";
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-11", new OpenAiResponsesUsage(1, 2, 3), raw);
		ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);
		when(openAiClient.createResponse(eq("activity"), eq("lesson"), modelCaptor.capture(),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);
		when(lessonGenJsonSupport.extractJsonPayload(raw)).thenReturn(Map.of("items", List.of()));

		// When:
		GeneratedActivityResult generated = service.generateLessonActivityPayload(prompt);

		// Then:
		assertThat(modelCaptor.getValue()).isEqualTo("gpt-4o");
		assertThat(generated.metadata().get("rawOutput")).isEqualTo(raw);
	}

	@Test
	void shouldThrowWhenActivityPayloadJsonIsInvalidTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "activity", "lesson");
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-12", new OpenAiResponsesUsage(1, 2, 3), "not-json");
		when(openAiClient.createResponse(eq("activity"), eq("lesson"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenReturn(result);
		when(lessonGenJsonSupport.extractJsonPayload("not-json")).thenReturn(null);

		// When-Then:
		assertThatThrownBy(() -> service.generateLessonActivityPayload(prompt))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("invalid activity JSON");
	}

	@Test
	void shouldDelegateExtractJsonPayloadToSupportBeanTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		Map<String, Object> parsed = Map.of("key", "value");
		when(lessonGenJsonSupport.extractJsonPayload("raw")).thenReturn(parsed);

		// When:
		Map<String, Object> result = service.extractJsonPayload("raw");

		// Then:
		assertThat(result).isSameAs(parsed);
	}

	@Test
	void shouldUploadFileAndReturnResponseTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		OpenAiFileUploadResponse response = new OpenAiFileUploadResponse("file-1", "user_data", 1024L);
		when(openAiClient.uploadFile(new byte[]{0, 1, 2}, "test.txt", "user_data")).thenReturn(response);

		// When:
		OpenAiFileUploadResponse result = service.uploadFile(new byte[]{0, 1, 2}, "test.txt", "user_data");

		// Then:
		assertThat(result).isSameAs(response);
	}

	@Test
	void shouldTranslateOpenAiExceptionToAppExceptionOnUploadTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		OpenAiExternalException cause = new OpenAiExternalException("upload failed", 500, "");
		when(openAiClient.uploadFile(new byte[]{0, 1, 2}, "test.txt", "user_data")).thenThrow(cause);

		// When-Then:
		assertThatThrownBy(() -> service.uploadFile(new byte[]{0, 1, 2}, "test.txt", "user_data"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("upload failed");
	}

	@Test
	void shouldThrowWhenOpenAiClientIsNotConfiguredTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(null);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions", "input");

		// When-Then:
		assertThatThrownBy(() -> service.generateLessonContent(prompt))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("OPENAI_API_KEY is not configured");
	}

	@Test
	void shouldTranslateOpenAiExceptionToAppExceptionOnCreateResponseTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions", "input");
		OpenAiExternalException cause = new OpenAiExternalException("network error", -1, "");
		when(openAiClient.createResponse(eq("instructions"), eq("input"), eq("gpt-4o-mini"),
				eq("cache-key"), eq(List.of()), isNull(), isNull()))
				.thenThrow(cause);

		// When-Then:
		assertThatThrownBy(() -> service.generateLessonContent(prompt))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("network error");
	}

	@Test
	void shouldSkipNullAndBlankFileInputsWhenCreatingResponseTest() {
		// Given:
		LessonGenServiceImpl service = service(defaultProperties());
		when(openAiClientProvider.getIfAvailable()).thenReturn(openAiClient);
		List<Map<String, Object>> rawFileInputs = List.of(
				Map.of("file_id", "file-1"),
				Map.of("file_id", ""),
				Map.of("file_id", "  "),
				Map.of("other", "ignored")
		);
		LessonGenPrompt prompt = new LessonGenPrompt("v1", "cache-key", "instructions", "input", rawFileInputs);
		OpenAiResponsesResult result = new OpenAiResponsesResult(
				"resp-13", new OpenAiResponsesUsage(1, 2, 3), "content");
		ArgumentCaptor<List<OpenAiFileInput>> captor = ArgumentCaptor.forClass(List.class);
		when(openAiClient.createResponse(eq("instructions"), eq("input"), eq("gpt-4o-mini"),
				eq("cache-key"), captor.capture(), isNull(), isNull()))
				.thenReturn(result);

		// When:
		GeneratedContentResult generated = service.generateLessonContent(prompt);

		// Then:
		assertThat(generated.content()).isEqualTo("content");
		List<OpenAiFileInput> captured = captor.getValue();
		assertThat(captured).hasSize(1);
		assertThat(captured.get(0).fileId()).isEqualTo("file-1");
	}
}
