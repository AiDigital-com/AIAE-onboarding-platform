package com.aidigital.aionboarding.service.storage;

import com.aidigital.aionboarding.domain.storage.entities.PendingUpload;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.external.storage.StorageClient;
import com.aidigital.aionboarding.external.storage.config.StorageProperties;
import com.aidigital.aionboarding.external.storage.models.ObjectMetadataRecord;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.observability.SecurityMetrics;
import com.aidigital.aionboarding.service.common.observability.enums.UploadRejectionReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.storage.enums.UploadPurpose;
import com.aidigital.aionboarding.service.storage.models.FilePreviewRecord;
import com.aidigital.aionboarding.service.storage.services.entity.PendingUploadEntityService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

	@Mock
	private StorageClient storageClient;
	@Mock
	private PendingUploadEntityService pendingUploadEntityService;
	@Mock
	private UserEntityService userEntityService;
	@Mock
	private StorageProperties properties;
	@Mock
	private SecurityMetrics securityMetrics;
	@Spy
	private CurrentTime currentTime = new CurrentTime();

	@InjectMocks
	private StorageService service;

	private AppUser viewer(Long id) {
		return new AppUser(id, "clerk-" + id, "user" + id + "@test.com", "User " + id, "member", "User", null, null,
				null);
	}

	private PendingUpload pendingUploadFor(Long ownerId, String storageKey, long expectedSizeBytes,
										   LocalDateTime expiresAt, boolean confirmed) {
		PendingUpload pendingUpload = new PendingUpload();
		pendingUpload.setId(99L);
		pendingUpload.setStorageKey(storageKey);
		User owner = new User();
		owner.setId(ownerId);
		pendingUpload.setOwnerUser(owner);
		pendingUpload.setExpectedContentType("video/mp4");
		pendingUpload.setExpectedSizeBytes(expectedSizeBytes);
		pendingUpload.setConfirmed(confirmed);
		pendingUpload.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
		pendingUpload.setExpiresAt(expiresAt);
		return pendingUpload;
	}

	@Nested
	class PresignGet {

		@Test
		void shouldUseConfiguredPreviewLifetimeTest() {
			// Given:
			when(properties.getPresignGetExpiresSeconds()).thenReturn(3600);
			when(storageClient.presignGet("uploads/abc/clip.mp4", Duration.ofSeconds(3600)))
					.thenReturn("https://cdn.example.com/uploads/abc/clip.mp4?Expires=3600");

			// When:
			String result = service.presignGet("uploads/abc/clip.mp4");

			// Then:
			assertThat(result).isEqualTo("https://cdn.example.com/uploads/abc/clip.mp4?Expires=3600");
			verify(storageClient).presignGet("uploads/abc/clip.mp4", Duration.ofSeconds(3600));
		}
	}

	@Nested
	class PresignGetBatch {

		@Test
		void shouldReturnEmptyListWhenKeysIsNullTest() {
			// When:
			List<FilePreviewRecord> result = service.presignGet((List<String>) null);

			// Then:
			assertThat(result).isEmpty();
			verifyNoInteractions(storageClient);
		}

		@Test
		void shouldDedupeBlankAndDuplicateKeysPreservingFirstSeenOrderTest() {
			// Given:
			when(properties.getPresignGetExpiresSeconds()).thenReturn(3600);
			when(storageClient.presignGet("uploads/a.png", Duration.ofSeconds(3600)))
					.thenReturn("https://cdn.example.com/uploads/a.png");
			when(storageClient.presignGet("uploads/b.png", Duration.ofSeconds(3600)))
					.thenReturn("https://cdn.example.com/uploads/b.png");
			List<String> keys = List.of("uploads/a.png", "", "uploads/b.png", "uploads/a.png", "   ");

			// When:
			List<FilePreviewRecord> result = service.presignGet(keys);

			// Then:
			assertThat(result).containsExactly(
					new FilePreviewRecord("uploads/a.png", "https://cdn.example.com/uploads/a.png"),
					new FilePreviewRecord("uploads/b.png", "https://cdn.example.com/uploads/b.png"));
		}

		@Test
		void shouldCapBatchAtMaxAllowedKeysTest() {
			// Given:
			when(properties.getPresignGetExpiresSeconds()).thenReturn(3600);
			when(storageClient.presignGet(anyString(), eq(Duration.ofSeconds(3600))))
					.thenAnswer(invocation -> "https://cdn.example.com/" + invocation.getArgument(0));
			List<String> keys = IntStream.range(0, 105).mapToObj(i -> "uploads/key-" + i).toList();

			// When:
			List<FilePreviewRecord> result = service.presignGet(keys);

			// Then:
			assertThat(result).hasSize(100);
			ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
			verify(storageClient, times(100)).presignGet(keyCaptor.capture(), eq(Duration.ofSeconds(3600)));
			assertThat(keyCaptor.getAllValues()).containsExactlyElementsOf(keys.subList(0, 100));
		}
	}

	@Nested
	class PresignPut {

		@Test
		void shouldThrowWhenSizeIsNotPositiveTest() {
			// Given:
			AppUser owner = viewer(1L);

			// When-Then:
			assertThatThrownBy(() -> service.presignPut(owner, UploadPurpose.LESSON_ASSET, "clip.mp4", "video/mp4",
					0L))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
		}

		@Test
		void shouldThrowWhenSizeExceedsConfiguredMaximumTest() {
			// Given:
			AppUser owner = viewer(2L);
			when(properties.getMaxUploadSizeBytes()).thenReturn(1000L);

			// When-Then:
			assertThatThrownBy(() -> service.presignPut(owner, UploadPurpose.LESSON_ASSET, "clip.mp4", "video/mp4",
					1001L))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
		}

		@Test
		void shouldPresignAndRecordAPendingUploadOwnedByTheCallerTest() {
			// Given:
			AppUser owner = viewer(3L);
			User ownerEntity = new User();
			ownerEntity.setId(3L);
			when(properties.getMaxUploadSizeBytes()).thenReturn(1_000_000L);
			when(properties.getPresignPutExpiresSeconds()).thenReturn(900);
			when(storageClient.presignPut(any(), any(), any())).thenReturn("https://bucket.example.com/presigned");
			when(userEntityService.getReference(3L)).thenReturn(ownerEntity);

			// When:
			StorageService.PresignedUpload result = service.presignPut(owner, UploadPurpose.LESSON_ASSET, "clip.mp4",
					"video/mp4", 500L);

			// Then:
			assertThat(result.uploadUrl()).isEqualTo("https://bucket.example.com/presigned");
			assertThat(result.storageKey()).startsWith("uploads/").endsWith("/clip.mp4");

			ArgumentCaptor<PendingUpload> captor = ArgumentCaptor.forClass(PendingUpload.class);
			verify(pendingUploadEntityService).save(captor.capture());
			PendingUpload saved = captor.getValue();
			assertThat(saved.getStorageKey()).isEqualTo(result.storageKey());
			assertThat(saved.getOwnerUser()).isSameAs(ownerEntity);
			assertThat(saved.getExpectedContentType()).isEqualTo("video/mp4");
			assertThat(saved.getExpectedSizeBytes()).isEqualTo(500L);
			assertThat(saved.isConfirmed()).isFalse();
			assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());
		}

		@Test
		void shouldThrowAndRecordAMetricWhenContentTypeIsNotAllowedForThePurposeTest() {
			// Given: MATERIAL_UPLOAD's allowlist is pdf/text/image — video is not permitted.
			AppUser owner = viewer(13L);
			when(properties.getMaxUploadSizeBytes()).thenReturn(1_000_000L);

			// When-Then:
			assertThatThrownBy(() -> service.presignPut(owner, UploadPurpose.MATERIAL_UPLOAD, "clip.mp4", "video/mp4",
					500L))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
			verify(securityMetrics).uploadRejected(UploadRejectionReason.MIME_NOT_ALLOWED);
		}

		@Test
		void shouldThrowWhenSizeExceedsThePurposesOwnCapEvenUnderTheGlobalMaximumTest() {
			// Given: AVATAR caps at 2 MB regardless of a much larger global maximum.
			AppUser owner = viewer(14L);
			when(properties.getMaxUploadSizeBytes()).thenReturn(500L * 1024 * 1024);

			// When-Then:
			assertThatThrownBy(() -> service.presignPut(owner, UploadPurpose.AVATAR, "avatar.png", "image/png",
					3L * 1024 * 1024))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
			verify(securityMetrics).uploadRejected(UploadRejectionReason.SIZE_EXCEEDED);
		}
	}

	@Nested
	class RequireOwnership {

		@Test
		void shouldPassWhenTheCallerOwnsTheConfirmedUploadTest() {
			// Given:
			AppUser owner = viewer(20L);
			PendingUpload pendingUpload = pendingUploadFor(20L, "avatar-key", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), true);
			when(pendingUploadEntityService.findByStorageKey("avatar-key")).thenReturn(Optional.of(pendingUpload));

			// When-Then: does not throw
			service.requireOwnership(owner, "avatar-key");
		}

		@Test
		void shouldThrowWhenTheCallerDoesNotOwnTheKeyTest() {
			// Given:
			AppUser owner = viewer(21L);
			PendingUpload pendingUpload = pendingUploadFor(999L, "someone-elses-key", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), true);
			when(pendingUploadEntityService.findByStorageKey("someone-elses-key")).thenReturn(Optional.of(pendingUpload));

			// When-Then:
			assertThatThrownBy(() -> service.requireOwnership(owner, "someone-elses-key"))
					.isInstanceOf(AppException.class);
			verify(securityMetrics).uploadRejected(UploadRejectionReason.OWNERSHIP_MISMATCH);
		}

		@Test
		void shouldThrowWhenTheKeyIsUnknownTest() {
			// Given:
			AppUser owner = viewer(22L);
			when(pendingUploadEntityService.findByStorageKey("missing-key")).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.requireOwnership(owner, "missing-key"))
					.isInstanceOf(AppException.class);
			verify(securityMetrics).uploadRejected(UploadRejectionReason.NOT_FOUND);
		}
	}

	@Nested
	class PutObject {

		@Test
		void shouldWriteBytesAndRegisterAnAlreadyConfirmedPendingUploadTest() {
			// Given: a direct backend-mediated upload (e.g. avatar) has no separate confirm step —
			// the backend itself just wrote the bytes, so ownership is established immediately.
			AppUser owner = viewer(30L);
			User ownerEntity = new User();
			ownerEntity.setId(30L);
			when(properties.getMaxUploadSizeBytes()).thenReturn(1_000_000L);
			when(properties.getPresignPutExpiresSeconds()).thenReturn(900);
			when(userEntityService.getReference(30L)).thenReturn(ownerEntity);
			byte[] bytes = new byte[]{1, 2, 3};

			// When:
			String storageKey = service.putObject(owner, UploadPurpose.AVATAR, bytes, "avatar.png", "image/png");

			// Then:
			verify(storageClient).putObject(storageKey, bytes, "image/png");
			ArgumentCaptor<PendingUpload> captor = ArgumentCaptor.forClass(PendingUpload.class);
			verify(pendingUploadEntityService).save(captor.capture());
			PendingUpload saved = captor.getValue();
			assertThat(saved.getStorageKey()).isEqualTo(storageKey);
			assertThat(saved.getOwnerUser()).isSameAs(ownerEntity);
			assertThat(saved.isConfirmed()).isTrue();
		}

		@Test
		void shouldRejectAnAvatarExceedingTheAllowlistTest() {
			// Given:
			AppUser owner = viewer(31L);
			when(properties.getMaxUploadSizeBytes()).thenReturn(1_000_000L);

			// When-Then:
			assertThatThrownBy(() -> service.putObject(owner, UploadPurpose.AVATAR, new byte[]{1}, "notes.pdf",
					"application/pdf"))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
		}
	}

	@Nested
	class ConfirmUpload {

		@Test
		void shouldThrowWhenStorageKeyIsBlankTest() {
			// Given:
			AppUser owner = viewer(4L);

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, ""))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldThrowWhenNoPendingUploadExistsTest() {
			// Given:
			AppUser owner = viewer(5L);
			when(pendingUploadEntityService.findByStorageKey("missing-key")).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, "missing-key"))
					.isInstanceOf(AppException.class);
		}

		@Test
		void shouldThrowWhenTheCallerIsNotTheOwnerTest() {
			// Given:
			AppUser owner = viewer(6L);
			PendingUpload pendingUpload = pendingUploadFor(999L, "key-1", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), false);
			when(pendingUploadEntityService.findByStorageKey("key-1")).thenReturn(Optional.of(pendingUpload));

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, "key-1"))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
		}

		@Test
		void shouldThrowWhenAlreadyConfirmedTest() {
			// Given:
			AppUser owner = viewer(7L);
			PendingUpload pendingUpload = pendingUploadFor(7L, "key-2", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), true);
			when(pendingUploadEntityService.findByStorageKey("key-2")).thenReturn(Optional.of(pendingUpload));

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, "key-2"))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
		}

		@Test
		void shouldThrowWhenExpiredTest() {
			// Given:
			AppUser owner = viewer(8L);
			PendingUpload pendingUpload = pendingUploadFor(8L, "key-3", 100L,
					LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), false);
			when(pendingUploadEntityService.findByStorageKey("key-3")).thenReturn(Optional.of(pendingUpload));

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, "key-3"))
					.isInstanceOf(AppException.class);
			verifyNoInteractions(storageClient);
		}

		@Test
		void shouldThrowWhenTheObjectNeverFinishedUploadingTest() {
			// Given:
			AppUser owner = viewer(9L);
			PendingUpload pendingUpload = pendingUploadFor(9L, "key-4", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), false);
			when(pendingUploadEntityService.findByStorageKey("key-4")).thenReturn(Optional.of(pendingUpload));
			when(storageClient.headObject("key-4")).thenReturn(Optional.empty());

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, "key-4"))
					.isInstanceOf(AppException.class);
			verify(pendingUploadEntityService, never()).save(any());
		}

		@Test
		void shouldThrowAndLeaveUnconfirmedWhenUploadedSizeDoesNotMatchTest() {
			// Given:
			AppUser owner = viewer(10L);
			PendingUpload pendingUpload = pendingUploadFor(10L, "key-5", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), false);
			when(pendingUploadEntityService.findByStorageKey("key-5")).thenReturn(Optional.of(pendingUpload));
			when(storageClient.headObject("key-5")).thenReturn(Optional.of(new ObjectMetadataRecord(50L,
					"video/mp4")));

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, "key-5"))
					.isInstanceOf(AppException.class);
			assertThat(pendingUpload.isConfirmed()).isFalse();
			verify(pendingUploadEntityService, never()).save(any());
		}

		@Test
		void shouldConfirmWhenOwnedNotExpiredAndSizeMatchesTest() {
			// Given:
			AppUser owner = viewer(11L);
			PendingUpload pendingUpload = pendingUploadFor(11L, "key-6", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), false);
			when(pendingUploadEntityService.findByStorageKey("key-6")).thenReturn(Optional.of(pendingUpload));
			when(storageClient.headObject("key-6")).thenReturn(Optional.of(new ObjectMetadataRecord(100L,
					"video/mp4")));
			when(pendingUploadEntityService.markConfirmedIfUnconfirmed("key-6")).thenReturn(true);

			// When-Then: does not throw
			service.confirmUpload(owner, "key-6");
		}

		@Test
		void shouldThrowWhenTheAtomicConfirmLosesTheRaceToAConcurrentCallerTest() {
			// Given: a second request confirmed this same key between our stale read and our
			// attempt to flip it, so the atomic conditional update matches zero rows.
			AppUser owner = viewer(15L);
			PendingUpload pendingUpload = pendingUploadFor(15L, "key-8", 100L,
					LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10), false);
			when(pendingUploadEntityService.findByStorageKey("key-8")).thenReturn(Optional.of(pendingUpload));
			when(storageClient.headObject("key-8")).thenReturn(Optional.of(new ObjectMetadataRecord(100L,
					"video/mp4")));
			when(pendingUploadEntityService.markConfirmedIfUnconfirmed("key-8")).thenReturn(false);

			// When-Then:
			assertThatThrownBy(() -> service.confirmUpload(owner, "key-8"))
					.isInstanceOf(AppException.class);
			verify(securityMetrics).uploadRejected(UploadRejectionReason.ALREADY_CONFIRMED);
		}
	}

	@Nested
	class CleanupAbandonedUploads {

		@Test
		void shouldReturnZeroAndSkipDeletesWhenNothingExpiredTest() {
			// Given:
			when(pendingUploadEntityService.findExpiredUnconfirmed(any(), any())).thenReturn(List.of());

			// When:
			int result = service.cleanupAbandonedUploads();

			// Then:
			assertThat(result).isZero();
			verifyNoInteractions(storageClient);
		}

		@Test
		void shouldDeleteRowsNowAndStorageObjectsAfterCommitTest() {
			// Given:
			PendingUpload expired = pendingUploadFor(12L, "key-7", 100L,
					LocalDateTime.now(ZoneOffset.UTC).minusHours(1), false);
			when(pendingUploadEntityService.findExpiredUnconfirmed(any(), any())).thenReturn(List.of(expired));

			try (MockedStatic<TransactionSynchronizationManager> txMgr =
						 mockStatic(TransactionSynchronizationManager.class)) {
				ArgumentCaptor<TransactionSynchronization> syncCaptor =
						ArgumentCaptor.forClass(TransactionSynchronization.class);

				// When:
				int result = service.cleanupAbandonedUploads();

				// Then:
				assertThat(result).isEqualTo(1);
				verify(pendingUploadEntityService).deleteAll(List.of(expired));
				verifyNoInteractions(storageClient);
				txMgr.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

				syncCaptor.getValue().afterCommit();
				verify(storageClient).deleteObjects(List.of("key-7"));
			}
		}
	}
}
