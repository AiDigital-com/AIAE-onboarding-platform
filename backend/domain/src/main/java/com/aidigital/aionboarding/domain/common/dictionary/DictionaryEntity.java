package com.aidigital.aionboarding.domain.common.dictionary;

import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base persistence model for dictionary tables.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class DictionaryEntity extends IdAwareEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
