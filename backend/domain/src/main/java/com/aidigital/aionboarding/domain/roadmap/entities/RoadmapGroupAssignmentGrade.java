package com.aidigital.aionboarding.domain.roadmap.entities;

import com.aidigital.aionboarding.domain.grade.entities.Grade;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

/**
 * One grade included in a {@link RoadmapGroupAssignment}'s filter. No rows for a given assignment
 * means "no grade filter" — every current and future group member is enrolled.
 */
@Entity
@Table(name = "roadmap_group_assignment_grades")
@Getter
@Setter
@NoArgsConstructor
public class RoadmapGroupAssignmentGrade {

    @EmbeddedId
    private RoadmapGroupAssignmentGradeId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("assignmentId")
    @JoinColumn(name = "assignment_id", nullable = false)
    private RoadmapGroupAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("gradeId")
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoadmapGroupAssignmentGradeId implements Serializable {

        @Column(name = "assignment_id")
        private Long assignmentId;

        @Column(name = "grade_id")
        private Long gradeId;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
                return false;
            }
            RoadmapGroupAssignmentGradeId that = (RoadmapGroupAssignmentGradeId) other;
            return Objects.equals(assignmentId, that.assignmentId) && Objects.equals(gradeId, that.gradeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(assignmentId, gradeId);
        }
    }
}
