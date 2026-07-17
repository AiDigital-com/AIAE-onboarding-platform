package com.aidigital.aionboarding.service.lesson.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.LessonAssetKindCode;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.lesson.entities.LessonAsset;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.lesson.models.CreateLessonAssetInput;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetDeleteResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonAssetResultRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonDetailRecord;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonAssetEntityService;
import com.aidigital.aionboarding.service.lesson.services.entity.LessonEntityService;
import com.aidigital.aionboarding.service.lesson.support.LessonAssetDraftBuilder;
import com.aidigital.aionboarding.service.lesson.support.LessonRecordAssembler;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonAssetServiceImplTest {

	@Mock
	private CurrentTime currentTime;
	@Mock
	private LessonEntityService lessonEntityService;
	@Mock
	private LessonAssetEntityService lessonAssetEntityService;
	@Mock
	private PermissionService permissionService;
	@Mock
	private LessonAssetDraftBuilder lessonAssetDraftBuilder;
	@Mock
	private LessonRecordAssembler lessonMapper;

	@InjectMocks
	private LessonAssetServiceImpl service;

	@Test
	void shouldCreateAssetTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Lesson lesson = lessonWithAuthor(1L, 1L);
		CreateLessonAssetInput input = new CreateLessonAssetInput(
				"link", "https://example.com", null, null, null, null,
				"Example", "Description", null, null, null
		);
		LessonAsset asset = new LessonAsset();
		asset.setId(100L);
		LessonAssetRecord assetRecord = new LessonAssetRecord(
				100L, 1L, LessonAssetKindCode.LINK, "Example", "Example", "https://example.com",
				"Description", null, null, null, "text/html", 0L, Map.of(), LocalDateTime.now()
		);
		LessonDetailRecord detailRecord = new LessonDetailRecord(
				1L, "Lesson", "Desc", "ready", "published", true, false,
				"", "standard", "clear", "article", "markdown", "", "",
				"", "", "", List.of(), Map.of(), List.of(), "",
				"Author", 1L, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				List.of(), List.of(), List.of(), null
		);
		when(lessonEntityService.getReference(1L)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);
		when(lessonAssetDraftBuilder.resolveAssetKind(eq(input))).thenReturn(LessonAssetKindCode.LINK);
		when(lessonAssetDraftBuilder.persistAsset(eq(viewer), eq(lesson), eq(input), eq(LessonAssetKindCode.LINK)))
				.thenReturn(asset);
		when(lessonMapper.toAssetRecord(eq(asset))).thenReturn(assetRecord);
		when(lessonMapper.toDetailRecord(eq(lesson))).thenReturn(detailRecord);

		// When:
		LessonAssetResultRecord result = service.createAsset(viewer, 1L, input);

		// Then:
		assertThat(result.asset()).isSameAs(assetRecord);
		assertThat(result.lesson()).isSameAs(detailRecord);
		verify(permissionService).requirePermission(viewer, PermissionKeys.LESSONS_MANAGE_ASSETS);
	}

	@Test
	void shouldDeleteAssetAndRemoveInlineReferenceTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Lesson lesson = lessonWithAuthor(1L, 1L);
		lesson.setContentHtml("<p>content</p><img data-storage-key=\"asset-key\" src=\"x.png\">");
		LessonAsset asset = new LessonAsset();
		asset.setId(100L);
		asset.setStorageKey("asset-key");
		asset.setLesson(lesson);
		LessonDetailRecord detailRecord = new LessonDetailRecord(
				1L, "Lesson", "Desc", "ready", "published", true, false,
				"", "standard", "clear", "article", "markdown", "",
				"", "", "", "", List.of(), Map.of(), List.of(), "",
				"Author", 1L, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				List.of(), List.of(), List.of(), null
		);
		when(lessonEntityService.getReference(1L)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);
		when(lessonAssetEntityService.findById(100L)).thenReturn(Optional.of(asset));
		when(currentTime.utcDateTime()).thenReturn(LocalDateTime.parse("2026-07-15T10:00:00"));
		when(lessonEntityService.save(eq(lesson))).thenReturn(lesson);
		when(lessonMapper.toDetailRecord(eq(lesson))).thenReturn(detailRecord);

		// When:
		LessonAssetDeleteResultRecord result = service.deleteAsset(viewer, 1L, 100L);

		// Then:
		assertThat(result.lesson()).isSameAs(detailRecord);
		verify(lessonAssetEntityService).delete(eq(asset));
		assertThat(lesson.getContentHtml()).isEqualTo("<p>content</p>");
		assertThat(lesson.getUpdatedAt()).isEqualTo("2026-07-15T10:00:00");
	}

	@Test
	void shouldDeleteAssetWithoutInlineReferenceTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Lesson lesson = lessonWithAuthor(1L, 1L);
		lesson.setContentHtml("<p>content</p>");
		LessonAsset asset = new LessonAsset();
		asset.setId(100L);
		asset.setStorageKey("asset-key");
		asset.setLesson(lesson);
		LessonDetailRecord detailRecord = new LessonDetailRecord(
				1L, "Lesson", "Desc", "ready", "published", true, false,
				"", "standard", "clear", "article", "markdown", "",
				"", "", "", "", List.of(), Map.of(), List.of(), "",
				"Author", 1L, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				List.of(), List.of(), List.of(), null
		);
		when(lessonEntityService.getReference(1L)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);
		when(lessonAssetEntityService.findById(100L)).thenReturn(Optional.of(asset));
		when(lessonMapper.toDetailRecord(eq(lesson))).thenReturn(detailRecord);

		// When:
		LessonAssetDeleteResultRecord result = service.deleteAsset(viewer, 1L, 100L);

		// Then:
		assertThat(result.lesson()).isSameAs(detailRecord);
		verify(lessonAssetEntityService).delete(eq(asset));
		verify(lessonEntityService, never()).save(eq(lesson));
	}

	@Test
	void shouldThrowWhenAssetNotFoundTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Lesson lesson = lessonWithAuthor(1L, 1L);
		when(lessonEntityService.getReference(1L)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);
		when(lessonAssetEntityService.findById(100L)).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> service.deleteAsset(viewer, 1L, 100L))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Lesson asset not found");
	}

	@Test
	void shouldThrowWhenAssetBelongsToDifferentLessonTest() {
		// Given:
		AppUser viewer = new AppUser(1L, "clerk-1", "admin@test.com", "Admin", "admin", "Admin", null, null, null);
		Lesson lesson = lessonWithAuthor(1L, 1L);
		Lesson otherLesson = lessonWithAuthor(2L, 1L);
		LessonAsset asset = new LessonAsset();
		asset.setId(100L);
		asset.setLesson(otherLesson);
		when(lessonEntityService.getReference(1L)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(1L))).thenReturn(true);
		when(lessonAssetEntityService.findById(100L)).thenReturn(Optional.of(asset));

		// When-Then:
		assertThatThrownBy(() -> service.deleteAsset(viewer, 1L, 100L))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Lesson asset not found");
	}

	@Test
	void shouldThrowWhenViewerCannotManageLessonTest() {
		// Given:
		AppUser viewer = new AppUser(2L, "clerk-2", "member@test.com", "Member", "member", "Member", null, null, null);
		Lesson lesson = lessonWithAuthor(1L, 3L);
		when(lessonEntityService.getReference(1L)).thenReturn(lesson);
		when(permissionService.canManageExistingLesson(eq(viewer), eq(3L))).thenReturn(false);

		// When-Then:
		assertThatThrownBy(() -> service.createAsset(viewer, 1L, new CreateLessonAssetInput(
				"link", "https://example.com", null, null, null, null,
				"Example", "Description", null, null, null
		)))
				.isInstanceOf(AppException.class);
	}

	private Lesson lessonWithAuthor(Long lessonId, Long authorId) {
		Lesson lesson = new Lesson();
		lesson.setId(lessonId);
		User author = new User();
		author.setId(authorId);
		lesson.setCreatedByUser(author);
		return lesson;
	}
}
