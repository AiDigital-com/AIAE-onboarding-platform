package com.aidigital.aionboarding.jobs;

import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.storage.entities.PendingUpload;
import com.aidigital.aionboarding.domain.storage.repositories.PendingUploadRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Correctness proof for {@link PendingUploadRepository#claimExpiredUnconfirmed(LocalDateTime, int)}
 * — the {@code WHERE}/{@code ORDER BY}/{@code LIMIT} shape of the claim, run single-threaded
 * against H2 (PostgreSQL compatibility mode, {@code application-test.yml}).
 * <p>
 * This is deliberately <b>not</b> a concurrency test — see the equivalent, more detailed note on
 * {@code MaterialYoutubeUrlClaimIntegrationTest}, which found repeated concurrent
 * {@code SELECT ... FOR UPDATE SKIP LOCKED} claims against this same H2 instance to be flaky.
 * The authoritative concurrency proof for this claim is run against a real PostgreSQL instance
 * via {@code docker compose} and is recorded in {@code docs/aiae-migration-log.md} P7, not here.
 * <p>
 * Class-level {@code @Transactional} rolls each test back — this H2 database's
 * {@code DB_CLOSE_DELAY=-1} keeps it alive across every test class in the same Surefire JVM, so
 * without a rollback the first test's rows would still be present (and still match "expired and
 * unconfirmed") when the second test ran.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AbandonedUploadClaimIntegrationTest {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PendingUploadRepository pendingUploadRepository;

	@Test
	void claimShouldReturnOnlyExpiredUnconfirmedRowsBoundedByLimitTest() {
		// Given: two expired unconfirmed uploads, one not-yet-expired unconfirmed upload, and one
		// already-confirmed (and therefore never abandoned) upload past its expiry
		UserRole role = userRoleRepository.save(userRole("role-claim-a"));
		User owner = userRepository.save(user(role));
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		PendingUpload expiredA = pendingUploadRepository.save(
				pendingUpload(owner, "key-expired-a", now.minusMinutes(10), false));
		PendingUpload expiredB = pendingUploadRepository.save(
				pendingUpload(owner, "key-expired-b", now.minusMinutes(5), false));
		PendingUpload notYetExpired = pendingUploadRepository.save(
				pendingUpload(owner, "key-not-expired", now.plusMinutes(10), false));
		PendingUpload confirmedAndExpired = pendingUploadRepository.save(
				pendingUpload(owner, "key-confirmed", now.minusMinutes(10), true));

		// When: claiming with a limit smaller than the number of expired-unconfirmed rows
		List<PendingUpload> claimed = pendingUploadRepository.claimExpiredUnconfirmed(now, 1);

		// Then: exactly 1 row, one of the two genuinely expired-and-unconfirmed rows, never the
		// not-yet-expired or the already-confirmed one
		assertThat(claimed).hasSize(1);
		Set<Long> claimedIds = new HashSet<>();
		claimed.forEach(row -> claimedIds.add(row.getId()));
		assertThat(claimedIds).isSubsetOf(Set.of(expiredA.getId(), expiredB.getId()));
		assertThat(claimedIds).doesNotContain(notYetExpired.getId(), confirmedAndExpired.getId());
	}

	@Test
	void claimShouldReturnEmptyWhenNothingIsExpiredTest() {
		// Given: only a not-yet-expired unconfirmed upload
		UserRole role = userRoleRepository.save(userRole("role-claim-b"));
		User owner = userRepository.save(user(role));
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		pendingUploadRepository.save(pendingUpload(owner, "key-future", now.plusMinutes(10), false));

		// When:
		List<PendingUpload> claimed = pendingUploadRepository.claimExpiredUnconfirmed(now, 100);

		// Then:
		assertThat(claimed).isEmpty();
	}

	private UserRole userRole(String code) {
		UserRole role = new UserRole();
		role.setCode(code);
		role.setName(code);
		role.setDisplayOrder(1);
		role.setIsActive(true);
		return role;
	}

	private User user(UserRole role) {
		User user = new User();
		user.setName("Owner");
		user.setEmail(role.getCode() + "@concurrency-test.example.com");
		user.setRole(role);
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private PendingUpload pendingUpload(User owner, String storageKey, LocalDateTime expiresAt, boolean confirmed) {
		PendingUpload pendingUpload = new PendingUpload();
		pendingUpload.setStorageKey(storageKey);
		pendingUpload.setOwnerUser(owner);
		pendingUpload.setExpectedContentType("video/mp4");
		pendingUpload.setExpectedSizeBytes(100L);
		pendingUpload.setConfirmed(confirmed);
		pendingUpload.setCreatedAt(expiresAt.minusMinutes(10));
		pendingUpload.setExpiresAt(expiresAt);
		return pendingUpload;
	}
}
