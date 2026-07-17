package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.prompt.LessonPromptBuilder;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.DuplicateTitle;
import com.aidigital.aionboarding.service.material.models.DuplicateUrl;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.OverlapNotes;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.models.PreparationStats;
import com.aidigital.aionboarding.service.material.models.SignalItem;
import com.aidigital.aionboarding.service.material.models.SignalNotes;
import com.aidigital.aionboarding.service.material.models.SourceReferenceItem;
import com.aidigital.aionboarding.service.material.support.MaterialTextSizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonGenerationTranscriptCondenserTest {

	@Mock
	private LessonPromptBuilder lessonPromptBuilder;
	@Mock
	private LessonGenService lessonGenService;
	@Mock
	private MaterialTextSizer materialTextSizer;

	@InjectMocks
	private LessonGenerationTranscriptCondenser condenser;

	@Test
	void shouldReturnOriginalResultWhenNoTranscriptNeedsCondensationTest() {
		// Given:
		String text = "short transcript";
		PreparedMaterialsResult prepared = preparedResult(text, 100);
		when(materialTextSizer.count(any())).thenReturn(text.length());

		// When:
		PreparedMaterialsResult result = condenser.condense(prepared);

		// Then:
		assertThat(result).isSameAs(prepared);
		verify(lessonGenService, never()).condenseSourceText(any());
	}

	@Test
	void shouldCondenseLongTranscriptAndUpdateStatsAndReferencesTest() {
		// Given:
		String longText = "x".repeat(13000);
		PreparedMaterialsResult prepared = preparedResult(longText, 1);
		LessonGenPrompt prompt = new LessonGenPrompt("v", "k", "instr", "input");
		GeneratedContentResult condensed = new GeneratedContentResult("condensed text", Map.of());
		when(lessonPromptBuilder.buildTranscriptCondensationPrompt(eq(longText))).thenReturn(prompt);
		when(lessonGenService.condenseSourceText(eq(prompt))).thenReturn(condensed);
		when(materialTextSizer.count(any())).thenReturn(20);

		// When:
		PreparedMaterialsResult result = condenser.condense(prepared);

		// Then:
		assertThat(result.materials()).hasSize(1);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> transcripts = (List<Map<String, Object>>) result.materials().get(0).data().get("youtubeTranscripts");
		assertThat(transcripts.get(0).get("preparedText")).isEqualTo("condensed text");
		assertThat(transcripts.get(0).get("wasCondensed")).isEqualTo(true);
		assertThat(result.sourceReferences()).hasSize(1);
		assertThat(result.stats().combinedTextCharacters()).isEqualTo(20);
	}

	@Test
	void shouldAsMapListFilterNonMapEntriesTest() {
		// Given:
		List<Object> raw = java.util.Arrays.asList(Map.of("k", "v"), "not a map", null, List.of());

		// When:
		List<Map<String, Object>> result = condenser.asMapList(raw);

		// Then:
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).containsEntry("k", "v");
	}

	@Test
	void shouldReturnEmptyMapListForNonCollectionInputTest() {
		// When:
		List<Map<String, Object>> result = condenser.asMapList("not a collection");

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void shouldReturnStringValForNullInputTest() {
		// When:
		String result = condenser.stringVal(null);

		// Then:
		assertThat(result).isEmpty();
	}

	private PreparedMaterialsResult preparedResult(String transcriptText, int sourceNumber) {
		Map<String, Object> transcript = new LinkedHashMap<>();
		transcript.put("preparedText", transcriptText);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("youtubeTranscripts", List.of(transcript));
		MaterialPreparationItem item = new MaterialPreparationItem((long) sourceNumber, sourceNumber, data);
		SourceReferenceItem reference = new SourceReferenceItem((long) sourceNumber, sourceNumber, new LinkedHashMap<>(data));
		return new PreparedMaterialsResult(
				List.of(item),
				List.of(reference),
				List.of(),
				new SignalNotes(List.of(new SignalItem(sourceNumber, "example")), List.of()),
				new OverlapNotes(List.of(new DuplicateTitle("title")), List.of(new DuplicateUrl("url"))),
				new PreparationStats(1, transcriptText.length())
		);
	}
}
