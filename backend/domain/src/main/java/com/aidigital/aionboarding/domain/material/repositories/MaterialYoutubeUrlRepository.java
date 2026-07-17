package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.MaterialYoutubeUrl;
import org.springframework.data.domain.Pageable;
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

	@Query("""
			SELECT y FROM MaterialYoutubeUrl y
			WHERE y.title = '' AND y.thumbnailUrl = '' AND y.metadataError = ''
			""")
	java.util.List<MaterialYoutubeUrl> findWithMissingMetadata(Pageable pageable);
}
