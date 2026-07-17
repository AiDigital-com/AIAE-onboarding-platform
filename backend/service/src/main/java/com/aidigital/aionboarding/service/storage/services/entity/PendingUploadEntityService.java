package com.aidigital.aionboarding.service.storage.services.entity;

import com.aidigital.aionboarding.domain.storage.entities.PendingUpload;
import com.aidigital.aionboarding.domain.storage.repositories.PendingUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Short-transaction CRUD helpers for the {@link PendingUpload} entity.
 * <p>
 * This is the only service that may inject {@link PendingUploadRepository} directly.
 * All other services that require pending-upload data must depend on this service.
 */
@Service
@RequiredArgsConstructor
public class PendingUploadEntityService {

	private final PendingUploadRepository pendingUploadRepository;

	/**
	 * Finds a pending upload by its storage key.
	 *
	 * @param storageKey object storage key
	 * @return matching pending upload, when present
	 */
	@Transactional(readOnly = true)
	public Optional<PendingUpload> findByStorageKey(String storageKey) {
		return pendingUploadRepository.findByStorageKey(storageKey);
	}

	/**
	 * Loads a bounded page of unconfirmed pending uploads that expired before the given cutoff,
	 * for abandoned-object cleanup.
	 *
	 * @param cutoff   expiry cutoff instant
	 * @param pageable bounds the batch size
	 * @return matching pending uploads, oldest expiry first
	 */
	@Transactional(readOnly = true)
	public List<PendingUpload> findExpiredUnconfirmed(LocalDateTime cutoff, Pageable pageable) {
		return pendingUploadRepository.findByConfirmedFalseAndExpiresAtBefore(cutoff, pageable);
	}

	/**
	 * Persists a pending upload row.
	 *
	 * @param pendingUpload the pending upload to save
	 * @return the saved {@link PendingUpload}
	 */
	@Transactional
	public PendingUpload save(PendingUpload pendingUpload) {
		return pendingUploadRepository.save(pendingUpload);
	}

	/**
	 * Atomically consumes a pending upload, flipping {@code confirmed} from false to true only if
	 * it is still unconfirmed. Used instead of a read-then-save round trip so two requests racing
	 * to confirm the same storage key cannot both succeed.
	 *
	 * @param storageKey object storage key
	 * @return {@code true} if this call was the one that confirmed the row; {@code false} if it
	 * was already confirmed by another call
	 */
	@Transactional
	public boolean markConfirmedIfUnconfirmed(String storageKey) {
		return pendingUploadRepository.markConfirmedIfUnconfirmed(storageKey) > 0;
	}

	/**
	 * Deletes pending upload rows.
	 *
	 * @param pendingUploads pending uploads to delete
	 */
	@Transactional
	public void deleteAll(List<PendingUpload> pendingUploads) {
		pendingUploadRepository.deleteAll(pendingUploads);
	}
}
