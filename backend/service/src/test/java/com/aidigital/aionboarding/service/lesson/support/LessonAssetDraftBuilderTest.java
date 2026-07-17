package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonAssetKind;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssetEntityService;
import com.aidigital.aionboarding.service.link.services.LinkMetadataService;
import com.aidigital.aionboarding.service.storage.StorageService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonAssetDraftBuilderTest {

	@Mock
	private LinkMetadataService linkMetadataService;
	@Mock
	private YoutubeClient youtubeClient;
	@Mock
	private DictionaryLookupService dictionaryLookupService;
	@Mock
	private LessonAssetEntityService lessonAssetEntityService;
	@Mock
	private StorageService storageService;
	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private LessonAssetDraftBuilder builder;

	@Test
	void persistAssetShouldConfirmUploadForAnUploadedFileKindTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
		Lesson lesson = new Lesson();
		lesson.setId(10L);
		CreateLessonAssetInput input = new CreateLessonAssetInput(
				"video", "", "storage-key-1", "video/mp4", "clip.mp4", 1024L,
				"", "", "", "", null
		);
		LessonAssetKind kind = Instancio.create(LessonAssetKind.class);
		when(dictionaryLookupService.getLessonAssetKindReference("video")).thenReturn(kind);
		when(lessonAssetEntityService.save(any(LessonAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When:
		LessonAsset result = builder.persistAsset(viewer, lesson, input, "video");

		// Then:
		assertThat(result.getStorageKey()).isEqualTo("storage-key-1");
		verify(storageService).confirmUpload(viewer, "storage-key-1");
	}

	@Test
	void persistAssetShouldNotConfirmUploadForAYoutubeKindTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "viewer2@test.com", "Viewer2", "member", "Viewer2", null, null,
				null);
		Lesson lesson = new Lesson();
		lesson.setId(11L);
		CreateLessonAssetInput input = new CreateLessonAssetInput(
				"youtube", "https://youtu.be/xyz", "", "", "", null,
				"", "", "", "", null
		);
		LessonAssetKind kind = Instancio.create(LessonAssetKind.class);
		YoutubeOEmbedMetadata metadata = Instancio.create(YoutubeOEmbedMetadata.class);
		when(dictionaryLookupService.getLessonAssetKindReference("youtube")).thenReturn(kind);
		when(youtubeClient.fetchOembed("https://youtu.be/xyz")).thenReturn(metadata);
		when(lessonAssetEntityService.save(any(LessonAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When:
		builder.persistAsset(viewer, lesson, input, "youtube");

		// Then:
		verifyNoInteractions(storageService);
	}

	@Test
	void enrichDraftShouldThrowWhenUploadedFileKindHasNoStorageKeyTest() {
		// Given:
		AppUser viewer = new AppUser(3L, "clerk-3", "viewer3@test.com", "Viewer3", "member", "Viewer3", null, null,
				null);
		Lesson lesson = new Lesson();
		lesson.setId(12L);
		CreateLessonAssetInput input = new CreateLessonAssetInput(
				"file", "", "", "", "", null,
				"", "", "", "", null
		);

		// When-Then:
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.persistAsset(viewer, lesson, input, "file"))
				.isInstanceOf(com.aidigital.aionboarding.service.common.error.AppException.class);
		verifyNoInteractions(storageService);
	}
}
