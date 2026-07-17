package com.aidigital.aionboarding.domain.common.dictionary.entities;

import com.aidigital.aionboarding.domain.common.dictionary.DictionaryEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "user_role")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class UserRole extends DictionaryEntity {
}
