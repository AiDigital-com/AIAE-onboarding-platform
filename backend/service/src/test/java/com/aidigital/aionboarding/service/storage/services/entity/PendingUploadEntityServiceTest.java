package com.aidigital.aionboarding.service.storage.services.entity;

import com.aidigital.aionboarding.domain.storage.entities.PendingUpload;
import com.aidigital.aionboarding.domain.storage.repositories.PendingUploadRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingUploadEntityServiceTest {

	@Mock
	private PendingUploadRepository pendingUploadRepository;

	@InjectMocks
	private PendingUploadEntityService pendingUploadEntityService;

	@Test
	void findByStorageKeyShouldReturnRepositoryResultTest() {
		// Given:
		PendingUpload pendingUpload = Instancio.of(PendingUpload.class).set(field(PendingUpload::getId), 1L).create();
		when(pendingUploadRepository.findByStorageKey(eq("key-1"))).thenReturn(Optional.of(pendingUpload));

		// When:
		Optional<PendingUpload> result = pendingUploadEntityService.findByStorageKey("key-1");

		// Then:
		assertThat(result).contains(pendingUpload);
	}

	@Test
	void findExpiredUnconfirmedShouldDelegateToRepositoryWithCutoffAndPageTest() {
		// Given:
		LocalDateTime cutoff = LocalDateTime.of(2026, 1, 1, 0, 0);
		PendingUpload pendingUpload = Instancio.of(PendingUpload.class).set(field(PendingUpload::getId), 2L).create();
		PageRequest pageable = PageRequest.of(0, 100);
		when(pendingUploadRepository.findByConfirmedFalseAndExpiresAtBefore(eq(cutoff), eq(pageable)))
				.thenReturn(List.of(pendingUpload));

		// When:
		List<PendingUpload> result = pendingUploadEntityService.findExpiredUnconfirmed(cutoff, pageable);

		// Then:
		assertThat(result).containsExactly(pendingUpload);
	}

	@Test
	void saveShouldReturnRepositorySaveResultTest() {
		// Given:
		PendingUpload pendingUpload = Instancio.of(PendingUpload.class).set(field(PendingUpload::getId), 3L).create();
		when(pendingUploadRepository.save(eq(pendingUpload))).thenReturn(pendingUpload);

		// When:
		PendingUpload result = pendingUploadEntityService.save(pendingUpload);

		// Then:
		assertThat(result).isSameAs(pendingUpload);
	}

	@Test
	void markConfirmedIfUnconfirmedShouldReturnTrueWhenTheRepositoryFlippedARowTest() {
		// Given:
		when(pendingUploadRepository.markConfirmedIfUnconfirmed(eq("key-5"))).thenReturn(1);

		// When:
		boolean result = pendingUploadEntityService.markConfirmedIfUnconfirmed("key-5");

		// Then:
		assertThat(result).isTrue();
	}

	@Test
	void markConfirmedIfUnconfirmedShouldReturnFalseWhenTheRowWasAlreadyConfirmedTest() {
		// Given:
		when(pendingUploadRepository.markConfirmedIfUnconfirmed(eq("key-6"))).thenReturn(0);

		// When:
		boolean result = pendingUploadEntityService.markConfirmedIfUnconfirmed("key-6");

		// Then:
		assertThat(result).isFalse();
	}

	@Test
	void deleteAllShouldDelegateToRepositoryTest() {
		// Given:
		List<PendingUpload> pendingUploads = List.of(
				Instancio.of(PendingUpload.class).set(field(PendingUpload::getId), 4L).create()
		);

		// When:
		pendingUploadEntityService.deleteAll(pendingUploads);

		// Then:
		verify(pendingUploadRepository).deleteAll(pendingUploads);
	}
}
