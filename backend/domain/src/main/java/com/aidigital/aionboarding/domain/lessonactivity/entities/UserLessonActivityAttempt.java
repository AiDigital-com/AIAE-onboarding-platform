package com.aidigital.aionboarding.domain.lessonactivity.entities;

import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.entities.IdAwareEntity;
import com.aidigital.aionboarding.domain.lesson.entities.Lesson;
import com.aidigital.aionboarding.domain.user.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "user_lesson_activity_attempts")
@Getter
@Setter
@NoArgsConstructor
public class UserLessonActivityAttempt extends IdAwareEntity {

	@Column(name = "legacy_source_id", unique = true)
	private String legacySourceId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "activity_id", nullable = false)
	private LessonActivity activity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", nullable = false)
	private ActivityType type;

	@Column(name = "attempt_number", nullable = false)
	private Integer attemptNumber;

	private BigDecimal score;

	@Column(nullable = false)
	private Boolean passed;

	@Column(name = "correct_count", nullable = false)
	private Integer correctCount;

	@Column(name = "total_count", nullable = false)
	private Integer totalCount;

	@Column(name = "submitted_answers", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private List<Object> submittedAnswers;

	@Column(name = "results", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private List<Object> results;

	@Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> metadata;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
