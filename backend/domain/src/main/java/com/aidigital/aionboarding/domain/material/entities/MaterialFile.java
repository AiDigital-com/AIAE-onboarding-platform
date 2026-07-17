package com.aidigital.aionboarding.domain.material.entities;

import com.aidigital.aionboarding.domain.common.dictionary.entities.MaterialFileKind;
import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_files")
@Getter
@Setter
@NoArgsConstructor
public class MaterialFile extends IdAwareEntity {

    @Column(name = "legacy_source_id", unique = true)
    private String legacySourceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kind_id", nullable = false)
    private MaterialFileKind kind;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "openai_file_id")
    private String openaiFileId;

    @Column(name = "openai_file_purpose", nullable = false)
    private String openaiFilePurpose;

    @Column(name = "openai_file_status", nullable = false)
    private String openaiFileStatus;

    @Column(name = "openai_file_error", nullable = false)
    private String openaiFileError;

    @Column(name = "openai_uploaded_at")
    private LocalDateTime openaiUploadedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
