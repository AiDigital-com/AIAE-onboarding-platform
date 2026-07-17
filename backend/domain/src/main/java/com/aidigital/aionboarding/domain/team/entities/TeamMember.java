package com.aidigital.aionboarding.domain.team.entities;

import com.aidigital.aionboarding.domain.user.entities.User;
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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "team_members")
@Getter
@Setter
@NoArgsConstructor
public class TeamMember {

    @EmbeddedId
    private TeamMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("leadUserId")
    @JoinColumn(name = "lead_user_id", nullable = false)
    private User leadUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("memberUserId")
    @JoinColumn(name = "member_user_id", nullable = false)
    private User memberUser;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamMemberId implements Serializable {

        @Column(name = "lead_user_id")
        private Long leadUserId;

        @Column(name = "member_user_id")
        private Long memberUserId;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
                return false;
            }
            TeamMemberId that = (TeamMemberId) other;
            return Objects.equals(leadUserId, that.leadUserId)
                    && Objects.equals(memberUserId, that.memberUserId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(leadUserId, memberUserId);
        }
    }
}
