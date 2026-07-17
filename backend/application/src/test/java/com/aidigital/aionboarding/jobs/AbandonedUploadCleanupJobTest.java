package com.aidigital.aionboarding.jobs;

import com.aidigital.aionboarding.service.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AbandonedUploadCleanupJobTest {

	@Mock
	private StorageService storageService;

	@InjectMocks
	private AbandonedUploadCleanupJob job;

	@Test
	void cleanupAbandonedUploadsShouldDelegateToStorageServiceTest() {
		// Given / When:
		job.cleanupAbandonedUploads();

		// Then:
		verify(storageService).cleanupAbandonedUploads();
	}
}
