package com.aidigital.aionboarding.domain.common.entities;

import com.aidigital.aionboarding.domain.group.entities.GroupLead;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.learning.entities.UserLesson;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.lesson.entities.LessonMaterial;
import com.aidigital.aionboarding.domain.lessonactivity.entities.UserLessonActivityProgress;
import com.aidigital.aionboarding.domain.permission.entities.UserPermissionOverride;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapGroupAssignmentGrade;
import com.aidigital.aionboarding.domain.roadmap.entities.RoadmapLesson;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

/**
 * Verifies the equals()/hashCode() contract of every {@code @Embeddable} composite-key class
 * under {@code domain} in one place, rather than one near-identical test file per key. Every one
 * follows the same Hibernate-recommended pattern — {@code Hibernate.getClass(this) !=
 * Hibernate.getClass(other)} instead of a strict {@code getClass()} check, so equality still
 * holds across a Hibernate-proxied subclass — hence the shared {@link Warning#STRICT_INHERITANCE}
 * suppression: the class is intentionally not {@code final}, and the proxy-safe check is the
 * point, not an oversight.
 */
class CompositeKeyEqualsContractTest {

	@Test
	void teamMemberIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(TeamMember.TeamMemberId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void roadmapLessonIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(RoadmapLesson.RoadmapLessonId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void roadmapGroupAssignmentGradeIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(RoadmapGroupAssignmentGrade.RoadmapGroupAssignmentGradeId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void userPermissionOverrideIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(UserPermissionOverride.UserPermissionOverrideId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void userLessonActivityProgressIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(UserLessonActivityProgress.UserLessonActivityProgressId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void lessonMaterialIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(LessonMaterial.LessonMaterialId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void userRoadmapIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(UserRoadmap.UserRoadmapId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void userLessonIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(UserLesson.UserLessonId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void groupMemberIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(GroupMember.GroupMemberId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

	@Test
	void groupLeadIdShouldSatisfyEqualsContractTest() {
		EqualsVerifier.forClass(GroupLead.GroupLeadId.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}
}
