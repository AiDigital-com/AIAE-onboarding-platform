package com.aidigital.aionboarding.jobs;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.domain.material.repositories.MaterialYoutubeUrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Correctness proof for {@link MaterialYoutubeUrlRepository#claimMissingMetadataBatch(int)} —
 * the {@code WHERE}/{@code ORDER BY}/{@code LIMIT} shape of the claim, run single-threaded
 * against H2 (PostgreSQL compatibility mode, {@code application-test.yml}).
 * <p>
 * This is deliberately <b>not</b> a concurrency test. A genuinely concurrent version of this
 * test — two threads, a {@code CyclicBarrier} forcing both {@code SELECT ... FOR UPDATE SKIP
 * LOCKED} claims to run while the other's transaction was still open, run through this exact
 * Spring context/H2 instance — was written and run repeatedly and was flaky: on a fresh table
 * with six unlocked, matching rows and no prior claim in the whole test JVM, one caller's claim
 * sometimes came back empty while the other claimed everything, and re-running the identical
 * scenario a moment later (after a "warm-up" query) made it pass. That inconsistency is exactly
 * the risk {@code docs/migration-guardrails.md} names: "{@code SELECT ... FOR UPDATE SKIP
 * LOCKED} is PostgreSQL-specific and H2 will not exercise it, so plan your proof accordingly."
 * A flaky green is worse than an honest gap, so the concurrency claim is not made here. The
 * authoritative concurrency proof for this claim is run against a real PostgreSQL instance via
 * {@code docker compose} and is recorded in {@code docs/aiae-migration-log.md} P7, not here.
 * <p>
 * Class-level {@code @Transactional} rolls each test back — this H2 database's
 * {@code DB_CLOSE_DELAY=-1} keeps it alive across every test class in the same Surefire JVM, so
 * without a rollback one test's rows could still be present when another ran.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MaterialYoutubeUrlClaimIntegrationTest {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Autowired
	private MaterialRepository materialRepository;

	@Autowired
	private MaterialYoutubeUrlRepository materialYoutubeUrlRepository;

	@Test
	void claimShouldReturnOnlyRowsMissingAllThreeMetadataFieldsBoundedByLimitTest() {
		// Given: two rows missing metadata, one row that already has a title (not missing),
		// and a claim limit smaller than the number of missing rows
		Material material = materialRepository.save(material());
		MaterialYoutubeUrl missingA = materialYoutubeUrlRepository.save(missingRow(material, "https://youtu.be/a"));
		MaterialYoutubeUrl missingB = materialYoutubeUrlRepository.save(missingRow(material, "https://youtu.be/b"));
		MaterialYoutubeUrl missingC = materialYoutubeUrlRepository.save(missingRow(material, "https://youtu.be/c"));
		MaterialYoutubeUrl alreadyFetched = missingRow(material, "https://youtu.be/d");
		alreadyFetched.setTitle("Already fetched");
		materialYoutubeUrlRepository.save(alreadyFetched);

		// When: claiming a batch smaller than the number of missing rows
		List<MaterialYoutubeUrl> claimed = materialYoutubeUrlRepository.claimMissingMetadataBatch(2);

		// Then: exactly 2 rows returned, both genuinely missing, never the already-fetched one
		assertThat(claimed).hasSize(2);
		Set<Long> claimedIds = new HashSet<>();
		claimed.forEach(row -> claimedIds.add(row.getId()));
		assertThat(claimedIds).isSubsetOf(Set.of(missingA.getId(), missingB.getId(), missingC.getId()));
		assertThat(claimedIds).doesNotContain(alreadyFetched.getId());
	}

	@Test
	void claimShouldReturnEmptyWhenNothingIsMissingTest() {
		// Given: no rows exist for this fresh id space of the claim query's WHERE clause to match
		// (a row that already has metadata is excluded)
		Material material = materialRepository.save(material());
		MaterialYoutubeUrl alreadyFetched = missingRow(material, "https://youtu.be/already-has-metadata");
		alreadyFetched.setTitle("Already fetched");
		alreadyFetched.setThumbnailUrl("https://thumb");
		materialYoutubeUrlRepository.save(alreadyFetched);

		// When:
		List<MaterialYoutubeUrl> claimed = materialYoutubeUrlRepository.claimMissingMetadataBatch(12);

		// Then:
		assertThat(claimed).doesNotContain(alreadyFetched);
	}

	private Material material() {
		Material material = new Material();
		material.setTitle("Material with YouTube URLs");
		material.setDescription("description");
		material.setTextContent("");
		material.setCoverImageStorageKey("");
		material.setCoverImageOriginalName("");
		material.setCoverImageMimeType("");
		material.setTags(List.of());
		material.setCreatedBy("tester");
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		material.setCreatedAt(now);
		material.setUpdatedAt(now);
		return material;
	}

	private MaterialYoutubeUrl missingRow(Material material, String url) {
		MaterialYoutubeUrl row = new MaterialYoutubeUrl();
		row.setMaterial(material);
		row.setUrl(url);
		row.setSortOrder(0);
		row.setTitle("");
		row.setAuthorName("");
		row.setAuthorUrl("");
		row.setThumbnailUrl("");
		row.setProviderName("");
		row.setMetadataError("");
		return row;
	}
}
