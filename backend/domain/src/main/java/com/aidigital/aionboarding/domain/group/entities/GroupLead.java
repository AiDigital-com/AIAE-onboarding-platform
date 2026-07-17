package com.aidigital.aionboarding.domain.group.entities;

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

/** One user's lead assignment to one group. A group may have more than one lead. */
@Entity
@Table(name = "group_leads")
@Getter
@Setter
@NoArgsConstructor
public class GroupLead {

    @EmbeddedId
    private GroupLeadId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("leadUserId")
    @JoinColumn(name = "lead_user_id", nullable = false)
    private User leadUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class GroupLeadId implements Serializable {

        @Column(name = "group_id")
        private Long groupId;

        @Column(name = "lead_user_id")
        private Long leadUserId;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
                return false;
            }
            GroupLeadId that = (GroupLeadId) other;
            return Objects.equals(groupId, that.groupId) && Objects.equals(leadUserId, that.leadUserId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, leadUserId);
        }
    }
}
