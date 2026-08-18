package com.aidigital.aionboarding.jobs;

import com.aidigital.aionboarding.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically sweeps presigned uploads that were issued but never registered onto a
 * lesson/material entity, deleting their storage objects and tracking rows so an abandoned
 * upload does not sit in the bucket indefinitely.
 * <p>
 * {@link StorageService#cleanupAbandonedUploads()} claims its batch with
 * {@code FOR UPDATE SKIP LOCKED} so two nodes running this job at the same time claim and
 * delete disjoint rows instead of both sweeping the same batch.
 */
@Component
@RequiredArgsConstructor
public class AbandonedUploadCleanupJob {

	private final StorageService storageService;

	/**
	 * Sweeps one bounded batch of expired, unconfirmed pending uploads.
	 */
	@Scheduled(fixedDelay = 900_000)
	public void cleanupAbandonedUploads() {
		storageService.cleanupAbandonedUploads();
	}
}
