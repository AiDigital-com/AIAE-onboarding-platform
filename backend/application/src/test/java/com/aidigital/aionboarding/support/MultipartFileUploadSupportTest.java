package com.aidigital.aionboarding.support;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.services.UploadValidator;
import com.aidigital.aionboarding.service.storage.StorageService;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultipartFileUploadSupportTest {

	@Mock
	private UploadValidator uploadValidator;
	@Mock
	private StorageService storageService;

	@InjectMocks
	private MultipartFileUploadSupport support;

	private AppUser viewer() {
		return new AppUser(1L, "clerk-1", "viewer@test.com", "Viewer", "member", "Viewer", null, null, null);
	}

	@Nested
	class RequireNonEmpty {

		@Test
		void shouldThrowWithTheGivenMessageWhenFileIsNullTest() {
			// When-Then:
			assertThatThrownBy(() -> support.requireNonEmpty(null, "Avatar file is required."))
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C002"));
		}

		@Test
		void shouldThrowWithTheGivenMessageWhenFileIsEmptyTest() {
			// Given:
			MultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

			// When-Then:
			assertThatThrownBy(() -> support.requireNonEmpty(empty, "file is required"))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldNotThrowForANonEmptyFileTest() {
			// Given:
			MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "content".getBytes());

			// When-Then: no exception
			support.requireNonEmpty(file, "file is required");
		}
	}

	@Nested
	class RequireValidFile {

		@Test
		void shouldDelegateMetadataValidationToUploadValidatorTest() {
			// Given:
			MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "content".getBytes());

			// When:
			support.requireValidFile(file);

			// Then:
			verify(uploadValidator).validate("a.txt", "text/plain", 7L);
		}

		@Test
		void shouldRejectAnEmptyFileBeforeDelegatingToUploadValidatorTest() {
			// Given:
			MultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

			// When-Then:
			assertThatThrownBy(() -> support.requireValidFile(empty)).isInstanceOf(AppException.class);
			verify(uploadValidator, never()).validate(any(), any(), org.mockito.ArgumentMatchers.anyLong());
		}
	}

	@Nested
	class PutStreaming {

		@Test
		void shouldStreamTheFilesOwnMetadataTest() {
			// Given:
			MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "content".getBytes());
			AppUser viewer = viewer();
			when(storageService.putObjectStreaming(eq(viewer), eq(UploadPurpose.MATERIAL_UPLOAD),
					any(InputStream.class), eq(7L), eq("a.pdf"), eq("application/pdf")))
					.thenReturn("uploads/a.pdf");

			// When:
			String result = support.putStreaming(viewer, UploadPurpose.MATERIAL_UPLOAD, file);

			// Then:
			assertThat(result).isEqualTo("uploads/a.pdf");
		}

		@Test
		void shouldStreamPreviouslyValidatedMetadataInsteadOfTheFilesOwnTest() {
			// Given: the validated metadata differs from the file's own to prove which one is used
			MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "content".getBytes());
			AppUser viewer = viewer();
			UploadValidator.UploadValidationRecord meta =
					new UploadValidator.UploadValidationRecord("renamed.pdf", "application/x-custom", 99L);
			when(storageService.putObjectStreaming(eq(viewer), eq(UploadPurpose.MATERIAL_UPLOAD),
					any(InputStream.class), eq(99L), eq("renamed.pdf"), eq("application/x-custom")))
					.thenReturn("uploads/renamed.pdf");

			// When:
			String result = support.putStreaming(viewer, UploadPurpose.MATERIAL_UPLOAD, file, meta);

			// Then:
			assertThat(result).isEqualTo("uploads/renamed.pdf");
		}

		@Test
		void shouldWrapAnIOExceptionFromReadingTheStreamTest() throws IOException {
			// Given:
			MultipartFile file = mock(MultipartFile.class);
			when(file.getInputStream()).thenThrow(new IOException("broken stream"));

			// When-Then:
			assertThatThrownBy(() -> support.putStreaming(viewer(), UploadPurpose.MATERIAL_UPLOAD, file))
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C000"));
		}
	}

	@Nested
	class PutBuffered {

		@Test
		void shouldUseTheFilesOriginalNameWhenPresentTest() throws IOException {
			// Given:
			MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "bytes".getBytes());
			AppUser viewer = viewer();
			when(storageService.putObject(viewer, UploadPurpose.AVATAR, "bytes".getBytes(), "avatar.png", "image/png"))
					.thenReturn("uploads/avatar.png");

			// When:
			String result = support.putBuffered(viewer, UploadPurpose.AVATAR, file, "fallback");

			// Then:
			assertThat(result).isEqualTo("uploads/avatar.png");
		}

		@Test
		void shouldFallBackToTheGivenNameWhenTheFileReportsNoneTest() throws IOException {
			// Given:
			MultipartFile file = mock(MultipartFile.class);
			when(file.getOriginalFilename()).thenReturn(null);
			when(file.getBytes()).thenReturn("bytes".getBytes());
			when(file.getContentType()).thenReturn("image/png");
			AppUser viewer = viewer();
			when(storageService.putObject(viewer, UploadPurpose.AVATAR, "bytes".getBytes(), "fallback", "image/png"))
					.thenReturn("uploads/fallback.png");

			// When:
			String result = support.putBuffered(viewer, UploadPurpose.AVATAR, file, "fallback");

			// Then:
			assertThat(result).isEqualTo("uploads/fallback.png");
		}

		@Test
		void shouldWrapAnIOExceptionFromReadingBytesTest() throws IOException {
			// Given:
			MultipartFile file = mock(MultipartFile.class);
			when(file.getBytes()).thenThrow(new IOException("broken bytes"));

			// When-Then:
			assertThatThrownBy(() -> support.putBuffered(viewer(), UploadPurpose.AVATAR, file, "fallback"))
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo("C000"));
		}
	}
}
