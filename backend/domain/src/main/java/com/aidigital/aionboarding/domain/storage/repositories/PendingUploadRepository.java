package com.aidigital.aionboarding.domain.storage.repositories;

import com.aidigital.aionboarding.domain.storage.entities.PendingUpload;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PendingUploadRepository extends JpaRepository<PendingUpload, Long> {

	Optional<PendingUpload> findByStorageKey(String storageKey);

	List<PendingUpload> findByConfirmedFalseAndExpiresAtBefore(LocalDateTime cutoff, Pageable pageable);

	/**
	 * Atomically flips {@code confirmed} from false to true for one storage key, so two
	 * concurrent confirmations racing on the same row cannot both observe an unconfirmed read and
	 * both proceed to register the upload: whichever transaction's UPDATE commits first wins, and
	 * the other necessarily matches zero rows once it re-evaluates {@code confirmed = false}
	 * against the now-committed row.
	 */
	@Modifying
	@Query("UPDATE PendingUpload p SET p.confirmed = true WHERE p.storageKey = :storageKey AND p.confirmed = false")
	int markConfirmedIfUnconfirmed(@Param("storageKey") String storageKey);
}
