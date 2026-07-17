package com.aidigital.aionboarding.domain.grade.entities;

import com.aidigital.aionboarding.domain.common.dictionary.DictionaryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Admin/team-lead-configurable grade dictionary entry (e.g. Junior, Middle, Senior).
 * <p>
 * Unlike the static dictionaries under {@code domain.common.dictionary}, grades are created,
 * renamed, and deactivated at runtime through {@code GradeService}, so this entity tracks its own
 * {@code createdAt}/{@code updatedAt} in addition to the shared {@link DictionaryEntity} columns.
 */
@Entity
@Table(name = "grades")
@Getter
@Setter
@NoArgsConstructor
public class Grade extends DictionaryEntity {

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
