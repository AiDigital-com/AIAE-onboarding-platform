package com.aidigital.aionboarding.team;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.domain.group.entities.Group;
import com.aidigital.aionboarding.domain.group.entities.GroupMember;
import com.aidigital.aionboarding.domain.group.repositories.GroupMemberRepository;
import com.aidigital.aionboarding.domain.group.repositories.GroupRepository;
import com.aidigital.aionboarding.domain.team.entities.TeamMember;
import com.aidigital.aionboarding.domain.team.repositories.TeamMemberRepository;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.domain.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the bounded team-lead search queries against real
 * PostgreSQL: {@link UserRepository#findGroupMemberUsersByGroupIdIn} must return each member at
 * most once even when they belong to more than one of the given groups, exclude the lead
 * themself, and honor search/paging; {@link TeamMemberRepository#findByLeadUserIdIn} must return
 * memberships only for the requested lead IDs.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TeamMemberSearchRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Test
	void findGroupMemberUsersByGroupIdInShouldReturnEachMemberOnceExcludingLeadAndMatchingSearchTest() {
		// Given: a lead leads groupA and groupB; memberAlice is in both (must appear once, not
		// twice), memberBob is only in groupB, the lead is also a member of groupA (must be
		// excluded), and memberOutside belongs to an unrelated group (must not appear)
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User lead = userRepository.save(user(role, "Lead", "lead@test.com"));
		User memberAlice = userRepository.save(user(role, "Alice", "alice@test.com"));
		User memberBob = userRepository.save(user(role, "Bob", "bob@test.com"));
		User memberOutside = userRepository.save(user(role, "Outsider", "outsider@test.com"));

		Group groupA = groupRepository.save(group("Group A", lead));
		Group groupB = groupRepository.save(group("Group B", lead));
		Group groupC = groupRepository.save(group("Group C", lead));
		groupMemberRepository.save(groupMember(groupA, memberAlice));
		groupMemberRepository.save(groupMember(groupA, lead));
		groupMemberRepository.save(groupMember(groupB, memberAlice));
		groupMemberRepository.save(groupMember(groupB, memberBob));
		groupMemberRepository.save(groupMember(groupC, memberOutside));

		// When: searching the lead's own groups (A and B) only
		Page<User> result = userRepository.findGroupMemberUsersByGroupIdIn(
				List.of(groupA.getId(), groupB.getId()), lead.getId(), null, PageRequest.of(0, 20));

		// Then: Alice appears once despite being in both groups; the lead and the unrelated
		// group's member are excluded
		assertThat(result.getContent()).extracting(User::getId)
				.containsExactlyInAnyOrder(memberAlice.getId(), memberBob.getId());
		assertThat(result.getTotalElements()).isEqualTo(2);
	}

	@Test
	void findGroupMemberUsersByGroupIdInShouldFilterBySearchAndPageTest() {
		// Given:
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User lead = userRepository.save(user(role, "Lead", "lead2@test.com"));
		User memberAlice = userRepository.save(user(role, "Alice Anderson", "alice2@test.com"));
		User memberBarry = userRepository.save(user(role, "Barry Brown", "barry@test.com"));
		Group group = groupRepository.save(group("Group X", lead));
		groupMemberRepository.save(groupMember(group, memberAlice));
		groupMemberRepository.save(groupMember(group, memberBarry));

		// When: searching for "ali" (matches only Alice)
		Page<User> result = userRepository.findGroupMemberUsersByGroupIdIn(
				List.of(group.getId()), lead.getId(), "%ali%", PageRequest.of(0, 20));

		// Then:
		assertThat(result.getContent()).extracting(User::getId).containsExactly(memberAlice.getId());
	}

	@Test
	void findByLeadUserIdInShouldReturnMembershipsOnlyForRequestedLeadIdsTest() {
		// Given: two leads, each with one team member
		UserRole role = userRoleRepository.findByCode(UserRoleCode.MEMBER).orElseThrow();
		User leadOne = userRepository.save(user(role, "Lead One", "leadone@test.com"));
		User leadTwo = userRepository.save(user(role, "Lead Two", "leadtwo@test.com"));
		User memberOfLeadOne = userRepository.save(user(role, "Member One", "memberone@test.com"));
		User memberOfLeadTwo = userRepository.save(user(role, "Member Two", "membertwo@test.com"));
		teamMemberRepository.save(teamMember(leadOne, memberOfLeadOne));
		teamMemberRepository.save(teamMember(leadTwo, memberOfLeadTwo));

		// When: requesting memberships for leadOne only
		List<TeamMember> result = teamMemberRepository.findByLeadUserIdIn(List.of(leadOne.getId()));

		// Then: leadTwo's membership is not loaded
		assertThat(result).extracting(tm -> tm.getId().getLeadUserId()).containsExactly(leadOne.getId());
		assertThat(result).extracting(tm -> tm.getMemberUser().getId()).containsExactly(memberOfLeadOne.getId());
	}

	private User user(UserRole role, String name, String email) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setRole(role);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);
		return user;
	}

	private Group group(String name, User createdBy) {
		Group group = new Group();
		group.setName(name);
		group.setNormalizedName(name.toLowerCase());
		group.setDescription("description");
		group.setCreatedByUser(createdBy);
		LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
		group.setCreatedAt(now);
		group.setUpdatedAt(now);
		return group;
	}

	private GroupMember groupMember(Group group, User member) {
		GroupMember gm = new GroupMember();
		GroupMember.GroupMemberId id = new GroupMember.GroupMemberId();
		id.setGroupId(group.getId());
		id.setMemberUserId(member.getId());
		gm.setId(id);
		gm.setGroup(group);
		gm.setMemberUser(member);
		gm.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return gm;
	}

	private TeamMember teamMember(User lead, User member) {
		TeamMember tm = new TeamMember();
		TeamMember.TeamMemberId id = new TeamMember.TeamMemberId();
		id.setLeadUserId(lead.getId());
		id.setMemberUserId(member.getId());
		tm.setId(id);
		tm.setLeadUser(lead);
		tm.setMemberUser(member);
		tm.setAddedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
		return tm;
	}
}
