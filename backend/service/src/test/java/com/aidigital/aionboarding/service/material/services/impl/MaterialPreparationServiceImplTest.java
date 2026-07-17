package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.service.common.mapping.JsonMapReader;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.material.models.PreparedMaterialsResult;
import com.aidigital.aionboarding.service.material.services.entity.MaterialEntityService;
import com.aidigital.aionboarding.service.material.support.MaterialPreparationMapBuilder;
import com.aidigital.aionboarding.service.material.support.MaterialTextSizer;
import com.aidigital.aionboarding.service.material.support.OverlapDetector;
import com.aidigital.aionboarding.service.material.support.SignalExtractor;
import com.aidigital.aionboarding.service.material.support.TermExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialPreparationServiceImplTest {

	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private MaterialEntityService materialEntityService;
	@Mock
	private MaterialPreparationMapBuilder materialPreparationMapBuilder;

	private final TermExtractor termExtractor = new TermExtractor();
	private final SignalExtractor signalExtractor = new SignalExtractor();
	private final OverlapDetector overlapDetector = new OverlapDetector();
	private final MaterialTextSizer materialTextSizer = new MaterialTextSizer(new JsonMapReader());

	@Test
	void shouldExtractTermsTest() {
		// Given:
		MaterialPreparationServiceImpl service = new MaterialPreparationServiceImpl(
				lessonEntityService, materialEntityService, materialPreparationMapBuilder,
				termExtractor, signalExtractor, overlapDetector, materialTextSizer);
		Material material = materialWithId(1L);
		when(materialEntityService.findById(1L)).thenReturn(material);
		when(materialPreparationMapBuilder.buildMaterialMap(eq(material), anyInt()))
				.thenReturn(materialMap("The Alpha Principle", "Overview of Alpha and Beta concepts."));

		// When:
		PreparedMaterialsResult result = service.prepareForMaterialIds(List.of(1L));

		// Then:
		assertThat(result.extractedTerms()).contains("Alpha", "Beta");
		assertThat(result.extractedTerms()).doesNotContain("The", "Overview");
		assertThat(result.extractedTerms().size()).isLessThanOrEqualTo(12);
	}

	@Test
	void shouldExtractSignalsTest() {
		// Given:
		MaterialPreparationServiceImpl service = new MaterialPreparationServiceImpl(
				lessonEntityService, materialEntityService, materialPreparationMapBuilder,
				termExtractor, signalExtractor, overlapDetector, materialTextSizer);
		Material material = materialWithId(2L);
		when(materialEntityService.findById(2L)).thenReturn(material);
		when(materialPreparationMapBuilder.buildMaterialMap(eq(material), anyInt()))
				.thenReturn(materialMap(
						"Signals",
						"Use Redis for caching. For example, Redis can store sessions. " +
								"However, be careful with memory limits."));

		// When:
		PreparedMaterialsResult result = service.prepareForMaterialIds(List.of(2L));

		// Then:
		assertThat(result.signals().examples()).hasSize(1);
		assertThat(result.signals().examples().get(0).text()).contains("For example");
		assertThat(result.signals().caveats()).hasSize(1);
		assertThat(result.signals().caveats().get(0).text()).contains("be careful");
		assertThat(result.signals().examples().get(0).sourceNumber()).isEqualTo(1);
	}

	@Test
	void shouldDetectOverlapsTest() {
		// Given:
		MaterialPreparationServiceImpl service = new MaterialPreparationServiceImpl(
				lessonEntityService, materialEntityService, materialPreparationMapBuilder,
				termExtractor, signalExtractor, overlapDetector, materialTextSizer);
		Material material1 = materialWithId(3L);
		Material material2 = materialWithId(4L);
		when(materialEntityService.findById(3L)).thenReturn(material1);
		when(materialEntityService.findById(4L)).thenReturn(material2);
		when(materialPreparationMapBuilder.buildMaterialMap(eq(material1), anyInt()))
				.thenReturn(materialMapWithUrls("Shared Title", List.of("https://example.com/a"), List.of()));
		when(materialPreparationMapBuilder.buildMaterialMap(eq(material2), anyInt()))
				.thenReturn(materialMapWithUrls("Shared Title", List.of("https://example.com/a"), List.of()));

		// When:
		PreparedMaterialsResult result = service.prepareForMaterialIds(List.of(3L, 4L));

		// Then:
		assertThat(result.overlaps().duplicateTitles()).hasSize(1);
		assertThat(result.overlaps().duplicateTitles().get(0).title()).isEqualTo("Shared Title");
		assertThat(result.overlaps().duplicateUrls()).hasSize(1);
		assertThat(result.overlaps().duplicateUrls().get(0).url()).isEqualTo("https://example.com/a");
	}

	@Test
	void shouldPreserveYoutubeTranscriptsTest() {
		// Given:
		MaterialPreparationServiceImpl service = new MaterialPreparationServiceImpl(
				lessonEntityService, materialEntityService, materialPreparationMapBuilder,
				termExtractor, signalExtractor, overlapDetector, materialTextSizer);
		Material material = materialWithId(5L);
		when(materialEntityService.findById(5L)).thenReturn(material);
		String longText = "x".repeat(12001);
		when(materialPreparationMapBuilder.buildMaterialMap(eq(material), anyInt()))
				.thenReturn(materialMapWithTranscript("Long transcript", longText));

		// When:
		PreparedMaterialsResult result = service.prepareForMaterialIds(List.of(5L));

		// Then:
		assertThat(result.sourceReferences()).hasSize(1);
		Object transcripts = result.sourceReferences().get(0).data().get("youtubeTranscripts");
		assertThat(transcripts).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> transcriptList = (List<Map<String, Object>>) transcripts;
		assertThat(transcriptList).hasSize(1);
		assertThat(transcriptList.get(0).get("preparedText")).isEqualTo(longText);
	}

	private Material materialWithId(Long id) {
		Material material = new Material();
		material.setId(id);
		return material;
	}

	private Map<String, Object> materialMap(String title, String description) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", 1L);
		map.put("sourceNumber", 1);
		map.put("title", title);
		map.put("description", description);
		map.put("text", "");
		map.put("youtubeUrls", List.of());
		map.put("youtubeVideos", List.of());
		map.put("youtubeTranscripts", List.of());
		map.put("links", List.of());
		map.put("linkAssets", List.of());
		map.put("attachments", List.of());
		return map;
	}

	private Map<String, Object> materialMapWithUrls(String title, List<String> links, List<String> youtubeUrls) {
		Map<String, Object> map = materialMap(title, "");
		map.put("links", links);
		map.put("youtubeUrls", youtubeUrls);
		return map;
	}

	private Map<String, Object> materialMapWithTranscript(String title, String preparedText) {
		Map<String, Object> map = materialMap(title, "");
		Map<String, Object> transcript = new LinkedHashMap<>();
		transcript.put("url", "https://youtube.com/watch?v=abc");
		transcript.put("preparedText", preparedText);
		transcript.put("wasCondensed", false);
		map.put("youtubeTranscripts", List.of(transcript));
		return map;
	}
}
