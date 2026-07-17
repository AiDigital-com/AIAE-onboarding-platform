package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.material.models.MaterialPreparationItem;
import com.aidigital.aionboarding.service.material.models.OverlapNotes;
import com.aidigital.aionboarding.service.material.models.PreparationStats;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.models.SignalNotes;
import com.aidigital.aionboarding.service.material.models.SourceReferenceItem;
import com.aidigital.aionboarding.service.material.services.MaterialPreparationService;
import com.aidigital.aionboarding.service.material.services.entity.MaterialEntityService;
import com.aidigital.aionboarding.service.material.support.MaterialPreparationMapBuilder;
import com.aidigital.aionboarding.service.material.support.MaterialTextSizer;
import com.aidigital.aionboarding.service.material.support.OverlapDetector;
import com.aidigital.aionboarding.service.material.support.SignalExtractor;
import com.aidigital.aionboarding.service.material.support.TermExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaterialPreparationServiceImpl implements MaterialPreparationService {

	private static final int MAX_SELECTED_MATERIALS = 8;
	private static final int MAX_COMBINED_TEXT_CHARACTERS = 80000;

	private final LessonEntityService lessonEntityService;
	private final MaterialEntityService materialEntityService;
	private final MaterialPreparationMapBuilder materialPreparationMapBuilder;
	private final TermExtractor termExtractor;
	private final SignalExtractor signalExtractor;
	private final OverlapDetector overlapDetector;
	private final MaterialTextSizer materialTextSizer;

	@Override
	@Transactional(readOnly = true)
	public PreparedMaterialsResult prepareForLesson(Long lessonId) {
		List<Long> materialIds = lessonEntityService.findLessonMaterialsByLessonId(lessonId).stream()
				.map(LessonMaterial::getMaterial)
				.map(Material::getId)
				.toList();
		return prepareForMaterialIds(materialIds);
	}

	@Override
	@Transactional(readOnly = true)
	public PreparedMaterialsResult prepareForMaterialIds(List<Long> materialIds) {
		List<MaterialPreparationItem> materials = new ArrayList<>();
		int index = 0;
		for (Long materialId : materialIds) {
			Material material = materialEntityService.findById(materialId);
			if (material == null) {
				continue;
			}
			index += 1;
			Map<String, Object> data = materialPreparationMapBuilder.buildMaterialMap(material, index);
			materials.add(new MaterialPreparationItem(material.getId(), index, data));
		}

		if (materials.size() > MAX_SELECTED_MATERIALS) {
			throw new AppException(ErrorReason.V001,
					"Select up to " + MAX_SELECTED_MATERIALS + " materials for one lesson.");
		}

		int combinedTextCharacters = materials.stream()
				.map(MaterialPreparationItem::data)
				.mapToInt(materialTextSizer::count)
				.sum();
		if (!materials.isEmpty() && combinedTextCharacters > MAX_COMBINED_TEXT_CHARACTERS) {
			throw new AppException(ErrorReason.V001,
					"Selected materials contain too much text. Limit: "
							+ MAX_COMBINED_TEXT_CHARACTERS + " characters.");
		}

		List<String> extractedTerms = termExtractor.extractTerms(materials);
		SignalNotes signals = new SignalNotes(
				signalExtractor.extractExamples(materials),
				signalExtractor.extractCaveats(materials));
		OverlapNotes overlaps = overlapDetector.detectOverlaps(materials);
		PreparationStats stats = new PreparationStats(materials.size(), combinedTextCharacters);
		List<SourceReferenceItem> sourceReferences = buildSourceReferences(materials);

		return new PreparedMaterialsResult(materials, sourceReferences, extractedTerms, signals, overlaps, stats);
	}

	/**
	 * Builds a list of source reference items from the prepared material items,
	 * carrying only the reference-relevant data fields (title, links, attachments,
	 * YouTube transcripts, etc.).
	 *
	 * @param materials prepared material items in source order
	 * @return source reference items in the same order
	 */
	List<SourceReferenceItem> buildSourceReferences(List<MaterialPreparationItem> materials) {
		return materials.stream().map(material -> {
			Map<String, Object> data = material.data();
			Map<String, Object> reference = new HashMap<>();
			reference.put("id", material.id());
			reference.put("sourceNumber", material.sourceNumber());
			reference.put("title", data.get("title"));
			reference.put("links", data.get("links"));
			reference.put("linkAssets", data.get("linkAssets"));
			reference.put("youtubeUrls", data.get("youtubeUrls"));
			reference.put("youtubeVideos", data.get("youtubeVideos"));
			reference.put("youtubeTranscripts", data.get("youtubeTranscripts"));
			reference.put("attachments", data.get("attachments"));
			return new SourceReferenceItem(material.id(), material.sourceNumber(), reference);
		}).toList();
	}

}
