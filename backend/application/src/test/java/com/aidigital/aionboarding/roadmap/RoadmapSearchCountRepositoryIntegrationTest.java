package com.aidigital.aionboarding.roadmap;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.learning.entities.UserRoadmap;
import com.aidigital.aionboarding.domain.learning.repositories.UserRoadmapRepository;
import com.aidigital.aionboarding.domain.roadmap.entities.Roadmap;
import com.aidigital.aionboarding.domain.roadmap.repositories.RoadmapRepository;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.team.repositories.TeamMemberRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import com.aidigital.aionboarding.service.common.mapping.TagsFilterSupport;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapListQuery;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapSortField;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapVisibilityFilter;
import com.aidigital.aionboarding.service.roadmap.support.RoadmapSpecificationBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link RoadmapSpecificationBuilder}'s visibility predicate — roadmaps are private by
 * default — against real PostgreSQL, reusing the same specification for both
 * {@code roadmapRepository.findAll(specification, pageable)} (the row query) and
 * {@code roadmapRepository.count(specification)} (the derived count query) so the tab-count total
 * and the active-tab list can never silently drift apart.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RoadmapSearchCountRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private RoadmapRepository roadmapRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private UserRoadmapRepository userRoadmapRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	private final RoadmapSpecificationBuilder specificationBuilder =
			new RoadmapSpecificationBuilder(new TagsFilterSupport(new ObjectMapper()));

	@Test
	void countShouldMatchTheNumberOfRoadmapsMatchingTheTagFilterForAnAdminViewerTest() {
		// Given: two roadmaps tagged "design", one untagged, viewed by an admin
		roadmapRepository.save(roadmap("Design Basics", List.of("design"), null));
		roadmapRepository.save(roadmap("Design Advanced", List.of("design"), null));
		roadmapRepository.save(roadmap("Unrelated", List.of(), null));

		RoadmapListQuery query = new RoadmapListQuery(
				null, List.of("design"), null, null, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);
		RoadmapVisibilityFilter admin = new RoadmapVisibilityFilter(true, false, false, 1L);
		Specification<Roadmap> specification = specificationBuilder.build(query, admin);

		// When:
		long total = roadmapRepository.count(specification);

		// Then: an admin sees every roadmap regardless of tag-independent visibility
		assertThat(total).isEqualTo(2L);
	}

	@Test
	void searchAndCountShouldExcludeUnassignedRoadmapsForAPlainMemberTest() {
		// Given: one roadmap the member is enrolled in, one they are not
		Roadmap assigned = roadmapRepository.save(roadmap("Assigned", List.of(), null));
		roadmapRepository.save(roadmap("Not Assigned", List.of(), null));
		User member = userRepository.save(user("Member", "member@test.com", UserRoleCode.MEMBER));
		userRoadmapRepository.save(enrollment(member, assigned, LocalDateTime.of(2026, 1, 1, 0, 0)));

		RoadmapListQuery query = anyRoadmapQuery();
		RoadmapVisibilityFilter memberVisibility = new RoadmapVisibilityFilter(false, false, false, member.getId());
		Specification<Roadmap> specification = specificationBuilder.build(query, memberVisibility);

		// When: listing and counting through the same specification
		var page = roadmapRepository.findAll(specification, PageRequest.of(0, 20));
		long total = roadmapRepository.count(specification);

		// Then: only the assigned roadmap is visible, and the count matches the list
		assertThat(page.getContent()).extracting(Roadmap::getTitle).containsExactly("Assigned");
		assertThat(total).isEqualTo(1L);
		assertThat(page.getTotalElements()).isEqualTo(1L);
	}

	@Test
	void searchShouldTreatAGroupFannedOutEnrollmentTheSameAsADirectAssignmentTest() {
		// Given: RoadmapGroupAssignmentSyncServiceImpl fans a group assignment out into one
		// UserRoadmap row per group member, so a group-assigned member's visibility is proven by
		// the same UserRoadmap EXISTS predicate as a direct assignment — no group-membership join
		// is exercised by the visibility predicate itself.
		Roadmap groupAssigned = roadmapRepository.save(roadmap("Group Rollout", List.of(), null));
		User groupMember = userRepository.save(user("GroupMember", "groupmember@test.com", UserRoleCode.MEMBER));
		userRoadmapRepository.save(enrollment(groupMember, groupAssigned, LocalDateTime.of(2026, 1, 1, 0, 0)));

		RoadmapListQuery query = anyRoadmapQuery();
		RoadmapVisibilityFilter memberVisibility =
				new RoadmapVisibilityFilter(false, false, false, groupMember.getId());
		Specification<Roadmap> specification = specificationBuilder.build(query, memberVisibility);

		// When:
		var page = roadmapRepository.findAll(specification, PageRequest.of(0, 20));

		// Then:
		assertThat(page.getContent()).extracting(Roadmap::getTitle).containsExactly("Group Rollout");
	}

	@Test
	void searchShouldIncludeOwnRoadmapForARoadmapManagerRegardlessOfEnrollmentTest() {
		// Given: a roadmap authored by a manage-holder who is not enrolled in it
		User manager = userRepository.save(user("Manager", "manager@test.com", UserRoleCode.TEAMLEAD));
		Roadmap ownRoadmap = roadmapRepository.save(roadmap("Own Roadmap", List.of(), manager));
		roadmapRepository.save(roadmap("Other Roadmap", List.of(), null));

		RoadmapListQuery query = anyRoadmapQuery();
		RoadmapVisibilityFilter managerVisibility = new RoadmapVisibilityFilter(false, true, false, manager.getId());
		Specification<Roadmap> specification = specificationBuilder.build(query, managerVisibility);

		// When:
		var page = roadmapRepository.findAll(specification, PageRequest.of(0, 20));

		// Then:
		assertThat(page.getContent()).extracting(Roadmap::getTitle).containsExactly("Own Roadmap");
	}

	@Test
	void searchShouldExcludeARoadmapWithNullAuthorFromTheOwnedBranchForAManagerTest() {
		// Given: a roadmap with a NULL authorUser (legacy/system-created) that must not
		// accidentally match a manager's "owned by viewer" branch, and must not null-out the
		// whole OR so the manager's other visible roadmaps still return
		User manager = userRepository.save(user("Manager2", "manager2@test.com", UserRoleCode.TEAMLEAD));
		roadmapRepository.save(roadmap("No Author", List.of(), null));
		Roadmap ownRoadmap = roadmapRepository.save(roadmap("Own Roadmap 2", List.of(), manager));

		RoadmapListQuery query = anyRoadmapQuery();
		RoadmapVisibilityFilter managerVisibility = new RoadmapVisibilityFilter(false, true, false, manager.getId());
		Specification<Roadmap> specification = specificationBuilder.build(query, managerVisibility);

		// When:
		var page = roadmapRepository.findAll(specification, PageRequest.of(0, 20));

		// Then: the NULL-author roadmap is excluded; the manager's own roadmap still returns
		assertThat(page.getContent()).extracting(Roadmap::getTitle).containsExactly("Own Roadmap 2");
	}

	@Test
	void searchShouldIncludeRoadmapAuthoredByOwnTeamMemberForATeamLeadTest() {
		// Given: a roadmap authored by a member of the team lead's own team
		User teamLead = userRepository.save(user("Lead", "lead@test.com", UserRoleCode.TEAMLEAD));
		User teamMemberAuthor = userRepository.save(user("Author", "author@test.com", UserRoleCode.MEMBER));
		teamMemberRepository.save(teamMembership(teamLead, teamMemberAuthor));
		Roadmap teamRoadmap = roadmapRepository.save(roadmap("Team Roadmap", List.of(), teamMemberAuthor));
		roadmapRepository.save(roadmap("Unrelated Roadmap", List.of(), null));

		RoadmapListQuery query = anyRoadmapQuery();
		RoadmapVisibilityFilter teamLeadVisibility = new RoadmapVisibilityFilter(false, true, true, teamLead.getId());
		Specification<Roadmap> specification = specificationBuilder.build(query, teamLeadVisibility);

		// When:
		var page = roadmapRepository.findAll(specification, PageRequest.of(0, 20));

		// Then:
		assertThat(page.getContent()).extracting(Roadmap::getTitle).containsExactly("Team Roadmap");
	}

	private RoadmapListQuery anyRoadmapQuery() {
		return new RoadmapListQuery(
				null, null, null, null, RoadmapSortField.CREATED_AT, Sort.Direction.DESC
		);
	}

	private Roadmap roadmap(String title, List<String> tags, User author) {
		Roadmap roadmap = new Roadmap();
		roadmap.setTitle(title);
		roadmap.setDescription("description");
		roadmap.setTags(tags);
		roadmap.setCreatedBy("tester");
		roadmap.setAuthorUser(author);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		roadmap.setCreatedAt(now);
		roadmap.setUpdatedAt(now);
		return roadmap;
	}

	private User user(String name, String email, String roleCode) {
		UserRole role = userRoleRepository.findByCode(roleCode).orElseThrow();
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private UserRoadmap enrollment(User user, Roadmap roadmap, LocalDateTime enrolledAt) {
		UserRoadmap userRoadmap = new UserRoadmap();
		UserRoadmap.UserRoadmapId id = new UserRoadmap.UserRoadmapId();
		id.setUserId(user.getId());
		id.setRoadmapId(roadmap.getId());
		userRoadmap.setId(id);
		userRoadmap.setUser(user);
		userRoadmap.setRoadmap(roadmap);
		userRoadmap.setEnrolledAt(enrolledAt);
		return userRoadmap;
	}

	private TeamMember teamMembership(User lead, User member) {
		TeamMember teamMember = new TeamMember();
		TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
		id.setLeadUserId(lead.getId());
		id.setMemberUserId(member.getId());
		teamMember.setId(id);
		teamMember.setLeadUser(lead);
		teamMember.setMemberUser(member);
		teamMember.setAddedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return teamMember;
	}
}
