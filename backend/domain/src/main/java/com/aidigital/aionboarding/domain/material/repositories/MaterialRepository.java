package com.aidigital.aionboarding.domain.material.repositories;

import com.aidigital.aionboarding.domain.material.entities.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material>,
		MaterialRepositoryCustom {

	@Query("SELECT COUNT(m) > 0 FROM Material m WHERE m.coverImageStorageKey = :storageKey AND m.coverImageStorageKey " +
			"<> ''")
	boolean existsByCoverImageStorageKey(@Param("storageKey") String storageKey);
}
