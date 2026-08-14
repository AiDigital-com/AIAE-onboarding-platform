package com.aidigital.aionboarding.service.team.services.impl;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.group.services.entity.GroupLeadEntityService;
import com.aidigital.aionboarding.service.group.services.entity.GroupMemberEntityService;
import com.aidigital.aionboarding.service.mappers.user.UserRecordMapper;
import com.aidigital.aionboarding.service.team.models.TeamRecord;
import com.aidigital.aionboarding.service.team.services.TeamService;
import com.aidigital.aionboarding.service.team.services.entity.TeamEntityService;
import com.aidigital.aionboarding.service.team.support.TeamMembershipSupport;
import com.aidigital.aionboarding.service.user.models.UserRecord;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

	private final UserEntityService userEntityService;
	private final TeamEntityService teamEntityService;
	private final GroupLeadEntityService groupLeadEntityService;
	private final GroupMemberEntityService groupMemberEntityService;
	private final UserRecordMapper userMapper;
	private final TeamMembershipSupport teamMembershipSupport;

	@Override
	@Transactional(readOnly = true)
	public List<TeamRecord> getTeams() {
		List<User> leads =
				userEntityService.findByRoleCodeIn(List.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD)).stream()
				.sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
				.toList();

		Map<Long, List<UserRecord>> membersByLead = membersByLeadId(leads.stream().map(User::getId).toList());

		return leads.stream()
				.map(lead -> new TeamRecord(
						userMapper.toRecord(lead),
						membersByLead.getOrDefault(lead.getId(), List.of()).stream()
								.sorted(Comparator.comparing(UserRecord::name, String.CASE_INSENSITIVE_ORDER))
								.toList()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TeamRecord> getTeams(AppUser user, String query, Pageable pageable) {
		if (user == null) {
			return Page.empty(pageable);
		}

		Page<User> leadPage;
		if (user.isAdmin()) {
			leadPage = userEntityService.findByRoleCodeIn(
					List.of(UserRoleCode.ADMIN, UserRoleCode.TEAMLEAD),
					query,
					pageable
			);
		} else if (user.isTeamLead()) {
			leadPage = singleVisibleLeadPage(user.internalId(), query, pageable);
		} else {
			return Page.empty(pageable);
		}

		Map<Long, List<UserRecord>> membersByLead =
				membersByLeadId(leadPage.getContent().stream().map(User::getId).toList());

		return leadPage.map(lead -> new TeamRecord(
				userMapper.toRecord(lead),
				membersByLead.getOrDefault(lead.getId(), List.of()).stream()
						.sorted(Comparator.comparing(UserRecord::name, String.CASE_INSENSITIVE_ORDER))
						.toList()
		));
	}

	/**
	 * Builds a lead-user-id to member-records map, restricted to team memberships led by the
	 * given lead IDs so cost is bounded by the number of leads being displayed (e.g. one results
	 * page) rather than every team membership in the workspace.
	 *
	 * @param leadUserIds lead user primary keys to load memberships for
	 * @return member records grouped by lead user id
	 */
	Map<Long, List<UserRecord>> membersByLeadId(Collection<Long> leadUserIds) {
		Map<Long, List<UserRecord>> membersByLead = new LinkedHashMap<>();
		teamEntityService.findByLeadUserIdIn(leadUserIds).forEach(tm ->
				membersByLead.computeIfAbsent(tm.getId().getLeadUserId(), k -> new ArrayList<>())
						.add(userMapper.toRecord(tm.getMemberUser())));
		return membersByLead;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserRecord> getAssignableLearningUsers(AppUser user) {
		if (user == null) {
			return List.of();
		}
		if (user.isAdmin()) {
			return userEntityService.findAll().stream()
					.filter(u -> !u.getId().equals(user.internalId()))
					.map(userMapper::toRecord)
					.toList();
		}
		if (!user.isTeamLead()) {
			return List.of();
		}
		return teamLedGroupMembers(user.internalId()).stream()
				.map(userMapper::toRecord)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<UserRecord> getAssignableLearningUsers(AppUser user, String query, Pageable pageable) {
		if (user == null) {
			return Page.empty(pageable);
		}
		if (user.isAdmin()) {
			return userEntityService.findAllExcluding(user.internalId(), query, pageable)
					.map(userMapper::toRecord);
		}
		if (!user.isTeamLead()) {
			return Page.empty(pageable);
		}
		Set<Long> groupIds = groupLeadEntityService.findGroupIdsByLeadUserId(user.internalId());
		return userEntityService.findGroupMemberUsersByGroupIdIn(groupIds, user.internalId(), query, pageable)
				.map(userMapper::toRecord);
	}

	/**
	 * Resolves the deduplicated members of every group the given user leads, via the current
	 * Group/GroupLead/GroupMember model. Excludes the lead themselves — a group's lead may also
	 * appear as one of its own members, but a lead does not manage learning assignments or
	 * permission overrides for themselves.
	 *
	 * @param leadUserId lead user primary key
	 * @return distinct member users across every group led by {@code leadUserId}
	 */
	List<User> teamLedGroupMembers(Long leadUserId) {
		Set<Long> groupIds = groupLeadEntityService.findGroupIdsByLeadUserId(leadUserId);
		Map<Long, User> membersById = new LinkedHashMap<>();
		groupMemberEntityService.findByGroupIdIn(groupIds).forEach(gm -> {
			User member = gm.getMemberUser();
			if (!member.getId().equals(leadUserId)) {
				membersById.putIfAbsent(member.getId(), member);
			}
		});
		return new ArrayList<>(membersById.values());
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserRecord> getTeamCandidateUsers(AppUser user) {
		if (user == null) {
			return List.of();
		}
		if (user.isAdmin()) {
			return userEntityService.findAllExcluding(user.internalId()).stream()
					.map(userMapper::toRecord)
					.toList();
		}
		if (!user.isTeamLead()) {
			return List.of();
		}
		return userEntityService.findTeamCandidates(user.internalId(), UserRoleCode.ADMIN).stream()
				.map(userMapper::toRecord)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<UserRecord> getTeamCandidateUsers(AppUser user, String query, Pageable pageable) {
		if (user == null) {
			return Page.empty(pageable);
		}
		if (user.isAdmin()) {
			return userEntityService.findAllExcluding(user.internalId(), query, pageable)
					.map(userMapper::toRecord);
		}
		if (!user.isTeamLead()) {
			return Page.empty(pageable);
		}
		return userEntityService.findTeamCandidates(user.internalId(), UserRoleCode.ADMIN, query, pageable)
				.map(userMapper::toRecord);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserRecord> getUserById(Long id) {
		return userEntityService.findById(id).map(userMapper::toRecord);
	}

	@Override
	@Transactional
	public UserRecord addTeamMember(Long leadUserId, Long memberUserId, String memberEmailOrName) {
		return teamMembershipSupport.addTeamMember(leadUserId, memberUserId, memberEmailOrName);
	}

	@Override
	@Transactional
	public boolean removeTeamMember(Long leadUserId, Long memberUserId) {
		return teamMembershipSupport.removeTeamMember(leadUserId, memberUserId);
	}

	Page<User> singleVisibleLeadPage(Long leadUserId, String query, Pageable pageable) {
		Optional<User> lead = userEntityService.findById(leadUserId)
				.filter(user -> matchesUserSearch(user, query));
		if (lead.isEmpty()) {
			return Page.empty(pageable);
		}
		long offset = pageable.isPaged() ? pageable.getOffset() : 0L;
		List<User> content = offset == 0L ? List.of(lead.get()) : List.of();
		return new PageImpl<>(content, pageable, 1L);
	}

	boolean matchesUserSearch(User user, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String normalized = query.trim().toLowerCase(Locale.ROOT);
		String name = user.getName() == null ? "" : user.getName().toLowerCase(Locale.ROOT);
		String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase(Locale.ROOT);
		return name.contains(normalized) || email.contains(normalized);
	}

}
