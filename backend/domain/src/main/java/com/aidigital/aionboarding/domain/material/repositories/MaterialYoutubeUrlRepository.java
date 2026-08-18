package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface MaterialYoutubeUrlRepository extends JpaRepository<MaterialYoutubeUrl, Long> {

	@Query("SELECT y FROM MaterialYoutubeUrl y WHERE y.material.id = :materialId ORDER BY y.sortOrder ASC")
	java.util.List<MaterialYoutubeUrl> findByMaterialIdOrderBySortOrderAsc(@Param("materialId") Long materialId);

	@Query("SELECT y FROM MaterialYoutubeUrl y WHERE y.material.id IN :materialIds ORDER BY y.material.id ASC, y" +
			".sortOrder ASC")
	java.util.List<MaterialYoutubeUrl> findByMaterialIdInOrderBySortOrderAsc(@Param("materialIds") Collection<Long> materialIds);

	void deleteByMaterial_Id(Long materialId);

	/**
	 * Atomically claims a bounded batch of rows still missing oEmbed metadata, using
	 * {@code FOR UPDATE SKIP LOCKED} so two nodes racing the same scheduled sweep lock and
	 * return disjoint rows instead of both selecting the same batch. Runs inside the short
	 * transaction the caller opens for the claim only; the row lock is released at that
	 * transaction's commit, before any external call is made.
	 * <p>
	 * PostgreSQL-specific syntax — {@code FOR UPDATE SKIP LOCKED} is not part of standard
	 * JPQL, hence the native query. See {@code docs/aiae-migration-log.md} P7 for how this is
	 * verified against both H2 (this module's test database) and a real PostgreSQL instance.
	 *
	 * @param limit maximum number of rows to claim in one call
	 * @return claimed rows, oldest id first; never more than {@code limit} rows
	 */
	@Query(
			value = "SELECT * FROM material_youtube_urls "
					+ "WHERE title = '' AND thumbnail_url = '' AND metadata_error = '' "
					+ "ORDER BY id ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
			nativeQuery = true
	)
	java.util.List<MaterialYoutubeUrl> claimMissingMetadataBatch(@Param("limit") int limit);
}
