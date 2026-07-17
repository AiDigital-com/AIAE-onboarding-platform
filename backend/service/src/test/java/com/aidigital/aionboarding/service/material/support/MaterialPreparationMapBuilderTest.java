package com.aidigital.aionboarding.service.material.support;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialFile;
import com.aidigital.aionboarding.domain.material.entities.MaterialLink;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptResult;
import com.aidigital.aionboarding.external.youtube.model.YoutubeTranscriptSegment;
import com.aidigital.aionboarding.service.lesson.util.LessonTextUtil;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialPreparationMapBuilderTest {

	@Mock
	private MaterialLinkService materialLinkService;
	@Mock
	private MaterialYoutubeService materialYoutubeService;
	@Mock
	private MaterialFileService materialFileService;
	@Mock
	private YoutubeClient youtubeClient;
	@Mock
	private LessonTextUtil lessonTextUtil;

	@InjectMocks
	private MaterialPreparationMapBuilder builder;

	@Test
	void buildMaterialMapShouldContainExpectedKeysFromServiceCollaboratorsTest() {
		// Given:
		Material material = Instancio.of(Material.class)
				.set(field(Material::getId), 70L)
				.set(field(Material::getTitle), "Material title")
				.create();
		MaterialLink link = Instancio.of(MaterialLink.class)
				.set(field(MaterialLink::getUrl), "https://example.com/link")
				.create();
		MaterialYoutubeUrl youtubeUrl = Instancio.of(MaterialYoutubeUrl.class)
				.set(field(MaterialYoutubeUrl::getUrl), "https://youtu.be/xyz789")
				.create();
		MaterialFile file = Instancio.of(MaterialFile.class)
				.set(field(MaterialFile::getOriginalName), "notes.pdf")
				.create();

		when(materialLinkService.findByMaterialIdOrderBySortOrderAsc(70L)).thenReturn(List.of(link));
		when(materialYoutubeService.findByMaterialIdOrderBySortOrderAsc(70L)).thenReturn(List.of(youtubeUrl));
		when(materialFileService.findByMaterialId(70L)).thenReturn(List.of(file));
		when(lessonTextUtil.normalizeText(org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		YoutubeTranscriptResult transcriptResult = new YoutubeTranscriptResult(
				"xyz789",
				List.of(new YoutubeTranscriptSegment(0.0, "Hello world")),
				null
		);
		when(youtubeClient.fetchTranscript("xyz789")).thenReturn(transcriptResult);

		// When:
		Map<String, Object> result = builder.buildMaterialMap(material, 1);

		// Then:
		assertThat(result).containsKeys(
				"id", "sourceNumber", "title", "youtubeUrls", "links", "linkAssets", "attachments"
		);
		assertThat(result.get("id")).isEqualTo(70L);
		assertThat(result.get("sourceNumber")).isEqualTo(1);
		assertThat(result.get("links")).isEqualTo(List.of("https://example.com/link"));
		assertThat(result.get("youtubeUrls")).isEqualTo(List.of("https://youtu.be/xyz789"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> attachments = (List<Map<String, Object>>) result.get("attachments");
		assertThat(attachments).hasSize(1);
		assertThat(attachments.get(0)).containsEntry("name", "notes.pdf");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> linkAssets = (List<Map<String, Object>>) result.get("linkAssets");
		assertThat(linkAssets).hasSize(1);
		assertThat(linkAssets.get(0)).containsEntry("url", "https://example.com/link");
	}
}
