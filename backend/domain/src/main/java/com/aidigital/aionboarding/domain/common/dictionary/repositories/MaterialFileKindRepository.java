package com.aidigital.aionboarding.domain.common.dictionary.repositories;

import com.aidigital.aionboarding.domain.common.dictionary.entities.MaterialFileKind;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;

public interface MaterialFileKindRepository extends JpaRepository<MaterialFileKind, Long> {

	@QueryHints({
			@QueryHint(name = HINT_CACHEABLE, value = "true"),
			@QueryHint(name = HINT_CACHE_REGION, value = "findMaterialFileKindByCode")
	})
	Optional<MaterialFileKind> findByCode(String code);
}
