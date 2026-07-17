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
@Table(name = "lesson_publication_status")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "com.aidigital.aionboarding.domain.common.dictionary" +
		".entities.LessonPublicationStatus")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class LessonPublicationStatus extends DictionaryEntity {

}
