package com.aidigital.aionboarding.jobs;

import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically backfills YouTube oEmbed metadata for material URLs still missing it.
 * <p>
 * Carries no {@code @Transactional}: {@link MaterialYoutubeService#backfillMissingYoutubeMetadata()}
 * claims its batch in one short transaction, calls YouTube with no transaction open, and
 * saves each result in its own short transaction, per {@code .claude/rules/14-performance.md}.
 * Wrapping this method in a transaction would reopen exactly that violation by holding one
 * connection for the whole batch, including every YouTube call.
 */
@Component
@RequiredArgsConstructor
public class MaterialYoutubeBackfillJob {

	private final MaterialYoutubeService materialYoutubeService;

	@Scheduled(fixedDelay = 300_000)
	public void backfillYoutubeMetadata() {
		materialYoutubeService.backfillMissingYoutubeMetadata();
	}
}
