package com.aidigital.aionboarding.service.material.validation;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.lesson.util.LessonTagUtil;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator.ValidatedMaterialPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialPayloadValidatorTest {

	@Mock
	private LessonTagUtil lessonTagUtil;

	@InjectMocks
	private MaterialPayloadValidator validator;

	@Test
	void shouldValidateCreateInputWithTextContentTest() {
		// Given:
		CreateMaterialInput input = new CreateMaterialInput(
				"Title",
				"Description",
				"body text",
				null,
				null,
				null,
				List.of("tag1", "tag2"),
				null,
				null,
				null
		);
		when(lessonTagUtil.normalizeLessonTagInput(eq(List.of("tag1", "tag2")))).thenReturn(List.of("tag1", "tag2"));

		// When:
		ValidatedMaterialPayload result = validator.validateCreateInput(input);

		// Then:
		assertThat(result.title()).isEqualTo("Title");
		assertThat(result.description()).isEqualTo("Description");
		assertThat(result.text()).isEqualTo("body text");
		assertThat(result.youtubeUrls()).isEmpty();
		assertThat(result.links()).isEmpty();
		assertThat(result.attachments()).isEmpty();
		assertThat(result.tags()).containsExactly("tag1", "tag2");
	}

	@Test
	void shouldValidateUpdateInputWithAttachmentsTest() {
		// Given:
		MaterialAttachmentInput attachment = new MaterialAttachmentInput(
				1L,
				"file.pdf",
				"key",
				"application/pdf",
				1024L,
				"file",
				null,
				null,
				null,
				null,
				null
		);
		UpdateMaterialInput input = new UpdateMaterialInput(
				"Updated",
				"",
				"",
				List.of("https://youtu.be/abc"),
				List.of("https://example.com"),
				List.of(attachment),
				List.of("  tag  "),
				null,
				null,
				null
		);
		when(lessonTagUtil.normalizeLessonTagInput(eq(List.of("  tag  ")))).thenReturn(List.of("tag"));

		// When:
		ValidatedMaterialPayload result = validator.validateUpdateInput(input);

		// Then:
		assertThat(result.title()).isEqualTo("Updated");
		assertThat(result.youtubeUrls()).containsExactly("https://youtu.be/abc");
		assertThat(result.links()).containsExactly("https://example.com");
		assertThat(result.attachments()).containsExactly(attachment);
		assertThat(result.tags()).containsExactly("tag");
	}

	@Test
	void shouldRejectBlankTitleTest() {
		// Given:
		CreateMaterialInput input = new CreateMaterialInput(
				"   ",
				null,
				"body",
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		// When-Then:
		assertThatThrownBy(() -> validator.validateCreateInput(input))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Title is required");
	}

	@Test
	void shouldRejectMissingContentSourceTest() {
		// Given:
		CreateMaterialInput input = new CreateMaterialInput(
				"Title",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
		when(lessonTagUtil.normalizeLessonTagInput(eq(null))).thenReturn(List.of());

		// When-Then:
		assertThatThrownBy(() -> validator.validateCreateInput(input))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("At least one content source is required");
	}

	@Test
	void shouldNormalizeBlankListInputsToEmptyTest() {
		// Given:
		CreateMaterialInput input = new CreateMaterialInput(
				"Title",
				null,
				"body",
				java.util.Arrays.asList("", "  "),
				java.util.Arrays.asList("  ", null),
				null,
				null,
				null,
				null,
				null
		);
		when(lessonTagUtil.normalizeLessonTagInput(eq(null))).thenReturn(List.of());

		// When:
		ValidatedMaterialPayload result = validator.validateCreateInput(input);

		// Then:
		assertThat(result.youtubeUrls()).isEmpty();
		assertThat(result.links()).isEmpty();
		assertThat(result.tags()).isEmpty();
	}
}
