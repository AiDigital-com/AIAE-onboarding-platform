package com.aidigital.aionboarding.jobs;

import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MaterialYoutubeBackfillJob {

	private final MaterialYoutubeService materialYoutubeService;

	@Scheduled(fixedDelay = 300_000)
	@Transactional
	public void backfillYoutubeMetadata() {
		materialYoutubeService.backfillMissingYoutubeMetadata();
	}
}
