package com.aidigital.aionboarding.domain.lesson.entities;

import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson extends IdAwareEntity {

    @Column(name = "legacy_source_id", unique = true)
    private String legacySourceId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private LessonStatus status;

    @Column(name = "user_instructions", nullable = false)
    private String userInstructions;

    @Column(nullable = false)
    private String depth;

    @Column(nullable = false)
    private String tone;

    @Column(name = "desired_format", nullable = false)
    private String desiredFormat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_format_id", nullable = false)
    private LessonContentFormat contentFormat;

    @Column(name = "content_markdown", nullable = false)
    private String contentMarkdown;

    @Column(name = "content_html", nullable = false)
    private String contentHtml;

    @Column(name = "cover_image_storage_key", nullable = false)
    private String coverImageStorageKey;

    @Column(name = "cover_image_original_name", nullable = false)
    private String coverImageOriginalName;

    @Column(name = "cover_image_mime_type", nullable = false)
    private String coverImageMimeType;

    @Column(name = "generation_metadata", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> generationMetadata;

    @Column(name = "revision_history", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Object> revisionHistory;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_status_id", nullable = false)
    private LessonPublicationStatus publicationStatus;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "tags", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> tags;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Optimistic-locking version. Multiple write paths can race on this row: manual content
     * edits, AI revision, inline-media cleanup on asset delete, and teacher-video generation.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}
