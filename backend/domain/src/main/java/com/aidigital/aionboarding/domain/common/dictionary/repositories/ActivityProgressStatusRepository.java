package com.aidigital.aionboarding.domain.common.dictionary.repositories;

import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityProgressStatus;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;

public interface ActivityProgressStatusRepository extends JpaRepository<ActivityProgressStatus, Long> {

	@QueryHints({
			@QueryHint(name = HINT_CACHEABLE, value = "true"),
			@QueryHint(name = HINT_CACHE_REGION, value = "findActivityProgressStatusByCode")
	})
	Optional<ActivityProgressStatus> findByCode(String code);
}
