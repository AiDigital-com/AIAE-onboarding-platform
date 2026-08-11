package com.aidigital.aionboarding.domain.storage.repositories;

import com.aidigital.aionboarding.domain.storage.entities.PendingUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PendingUploadRepository extends JpaRepository<PendingUpload, Long> {

	Optional<PendingUpload> findByStorageKey(String storageKey);

	/**
	 * Atomically claims a bounded batch of expired, unconfirmed pending uploads, using
	 * {@code FOR UPDATE SKIP LOCKED} so two nodes racing the same scheduled sweep lock and
	 * return disjoint rows instead of both selecting the same batch. The caller deletes the
	 * claimed rows in the same transaction as this claim (see
	 * {@code StorageService#cleanupAbandonedUploads()}), so — unlike the YouTube backfill —
	 * there is no window between claim and removal in which a second node could reclaim the
	 * same rows; the S3 object delete itself still happens after commit, outside any
	 * transaction.
	 * <p>
	 * PostgreSQL-specific syntax — {@code FOR UPDATE SKIP LOCKED} is not part of standard
	 * JPQL, hence the native query. See {@code docs/aiae-migration-log.md} P7 for how this is
	 * verified against both H2 (this module's test database) and a real PostgreSQL instance.
	 *
	 * @param cutoff expiry cutoff instant
	 * @param limit  maximum number of rows to claim
	 * @return claimed rows, oldest expiry first; never more than {@code limit} rows
	 */
	@Query(
			value = "SELECT * FROM pending_uploads "
					+ "WHERE confirmed = false AND expires_at < :cutoff "
					+ "ORDER BY expires_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
			nativeQuery = true
	)
	List<PendingUpload> claimExpiredUnconfirmed(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

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
