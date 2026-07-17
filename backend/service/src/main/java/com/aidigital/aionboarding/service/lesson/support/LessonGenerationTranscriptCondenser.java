package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.service.lessongen.model.GeneratedContentResult;
import com.aidigital.aionboarding.service.lessongen.model.LessonGenPrompt;
import com.aidigital.aionboarding.service.lessongen.prompt.LessonPromptBuilder;
import com.aidigital.aionboarding.service.lessongen.services.LessonGenService;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.PreparationStats;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.models.SourceReferenceItem;
import com.aidigital.aionboarding.service.material.support.MaterialTextSizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Condenses per-material YouTube transcripts that exceed the per-transcript
 * character threshold, using the LLM to shrink oversized transcript text before
 * it is included in a lesson-generation prompt.
 */
@Component
@RequiredArgsConstructor
public class LessonGenerationTranscriptCondenser {

	private static final int PER_TRANSCRIPT_CONDENSATION_THRESHOLD = 12_000;

	private final LessonPromptBuilder lessonPromptBuilder;
	private final LessonGenService lessonGenService;
	private final MaterialTextSizer materialTextSizer;

	/**
	 * Condenses per-transcript text that exceeds the per-transcript threshold using the LLM.
	 *
	 * @param prepared prepared materials result
	 * @return original or condensed prepared materials result
	 */
	public PreparedMaterialsResult condense(PreparedMaterialsResult prepared) {
		return condenseLongYoutubeTranscripts(prepared);
	}

	/**
	 * Condenses long YouTube transcript entries and rebuilds stats/source references when changed.
	 *
	 * @param prepared prepared materials result
	 * @return original or condensed prepared materials result
	 */
	PreparedMaterialsResult condenseLongYoutubeTranscripts(PreparedMaterialsResult prepared) {
		boolean condensedAny = false;
		List<MaterialPreparationItem> updatedItems = new ArrayList<>();
		int combinedTextCharacters = 0;

		for (MaterialPreparationItem item : prepared.materials()) {
			Map<String, Object> data = new LinkedHashMap<>(item.data());
			List<Map<String, Object>> transcripts = asMapList(data.get("youtubeTranscripts"));
			if (!transcripts.isEmpty()) {
				List<Map<String, Object>> condensedTranscripts = new ArrayList<>();
				for (Map<String, Object> transcript : transcripts) {
					String text = stringVal(transcript.get("preparedText"));
					if (text.length() > PER_TRANSCRIPT_CONDENSATION_THRESHOLD) {
						LessonGenPrompt condensationPrompt =
								lessonPromptBuilder.buildTranscriptCondensationPrompt(text);
						GeneratedContentResult condensed = lessonGenService.condenseSourceText(condensationPrompt);
						Map<String, Object> ct = new LinkedHashMap<>(transcript);
						ct.put("preparedText", condensed.content());
						ct.put("wasCondensed", true);
						condensedTranscripts.add(ct);
						condensedAny = true;
					} else {
						condensedTranscripts.add(transcript);
					}
				}
				data.put("youtubeTranscripts", condensedTranscripts);
			}
			updatedItems.add(new MaterialPreparationItem(item.id(), item.sourceNumber(), data));
			combinedTextCharacters += materialTextSizer.count(data);
		}

		if (!condensedAny) {
			return prepared;
		}

		Map<Integer, MaterialPreparationItem> bySourceNumber = updatedItems.stream()
				.collect(Collectors.toMap(MaterialPreparationItem::sourceNumber, i -> i));
		List<SourceReferenceItem> updatedReferences = prepared.sourceReferences().stream()
				.map(ref -> {
					MaterialPreparationItem updated = bySourceNumber.get(ref.sourceNumber());
					return updated != null
							? new SourceReferenceItem(ref.id(), ref.sourceNumber(), updated.data())
							: ref;
				})
				.toList();

		PreparationStats newStats = new PreparationStats(updatedItems.size(), combinedTextCharacters);
		return new PreparedMaterialsResult(
				updatedItems,
				updatedReferences,
				prepared.extractedTerms(),
				prepared.signals(),
				prepared.overlaps(),
				newStats
		);
	}

	/**
	 * Safely casts a raw collection to a list of string-keyed maps, skipping non-map entries.
	 *
	 * @param value raw collection value
	 * @return filtered map list
	 */
	@SuppressWarnings("unchecked")
	List<Map<String, Object>> asMapList(Object value) {
		if (!(value instanceof Collection<?> collection)) {
			return List.of();
		}
		List<Map<String, Object>> maps = new ArrayList<>();
		for (Object item : collection) {
			if (item instanceof Map<?, ?> map) {
				maps.add((Map<String, Object>) map);
			}
		}
		return maps;
	}

	/**
	 * Returns a trimmed string from any object, or empty string for null.
	 *
	 * @param value raw value
	 * @return trimmed string or empty string
	 */
	String stringVal(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
