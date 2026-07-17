package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.PermissionDefinitionMetaV1;
import com.aidigital.aionboarding.api.v1.model.PermissionDefinitionV1;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring-managed catalog of the application's permission definitions, exposed via an instance
 * accessor for injection into MapStruct mappers and controllers.
 */
@Component
public class PermissionDefinitionRegistry {

	private final List<PermissionDefinitionMetaV1> all = List.of(
			meta(PermissionDefinitionV1.ADMIN_MANAGE_ROLES, "Admin", "Manage roles", "Assign and remove team lead " +
					"roles."),
			meta(PermissionDefinitionV1.PERMISSIONS_MANAGE_TEAMLEADS, "Admin", "Manage team lead permissions", "Change" +
					" permission overrides for team leads."),
			meta(PermissionDefinitionV1.PERMISSIONS_MANAGE_TEAM_MEMBERS, "Admin", "Manage team member permissions",
					"Change permission overrides for members of an owned team."),
			meta(PermissionDefinitionV1.TEAMS_MANAGE_MEMBERS, "Teams", "Manage team members", "Add and remove members " +
					"in manageable teams."),
			meta(PermissionDefinitionV1.GROUPS_MANAGE, "Groups", "Manage groups", "Create, delete, and manage " +
					"membership/leads for any group."),
			meta(PermissionDefinitionV1.GRADES_MANAGE, "Grades", "Manage grade values", "Create, rename, and " +
					"deactivate grade dictionary values."),
			meta(PermissionDefinitionV1.MATERIALS_CREATE, "Materials", "Create materials", null),
			meta(PermissionDefinitionV1.MATERIALS_EDIT, "Materials", "Edit materials", null),
			meta(PermissionDefinitionV1.MATERIALS_DELETE, "Materials", "Delete materials", null),
			meta(PermissionDefinitionV1.LESSONS_CREATE, "Lessons", "Create lessons", null),
			meta(PermissionDefinitionV1.LESSONS_MANAGE, "Lessons", "Edit/delete lessons", null),
			meta(PermissionDefinitionV1.LESSONS_PUBLISH_ARCHIVE, "Lessons", "Publish/archive lessons", null),
			meta(PermissionDefinitionV1.LESSONS_MANAGE_ACTIVITIES, "Lessons", "Manage activities", null),
			meta(PermissionDefinitionV1.LESSONS_MANAGE_ASSETS, "Lessons", "Manage lesson assets", null),
			meta(PermissionDefinitionV1.ROADMAPS_CREATE, "Roadmaps", "Create roadmaps", null),
			meta(PermissionDefinitionV1.ROADMAPS_MANAGE, "Roadmaps", "Edit/delete roadmaps", null),
			meta(PermissionDefinitionV1.LEARNING_ENROLL, "Learning", "Enroll in lessons/roadmaps", null),
			meta(PermissionDefinitionV1.LEARNING_ASSIGN, "Learning", "Assign lessons/roadmaps", null),
			meta(PermissionDefinitionV1.LEARNING_COMPLETE, "Learning", "Complete lessons/activities", null),
			meta(PermissionDefinitionV1.LEARNING_ASK, "Learning", "Ask lesson assistant", null)
	);

	/**
	 * Returns the full, ordered catalog of permission definitions.
	 *
	 * @return the 20 permission definitions in their canonical display order
	 */
	public List<PermissionDefinitionMetaV1> all() {
		return all;
	}

	/**
	 * Builds a single permission definition entry.
	 *
	 * @param code        the permission code
	 * @param group       the display group the permission belongs to
	 * @param label       the human-readable label
	 * @param description an optional human-readable description
	 * @return the assembled permission definition DTO
	 */
	PermissionDefinitionMetaV1 meta(PermissionDefinitionV1 code, String group, String label, String description) {
		PermissionDefinitionMetaV1 meta = new PermissionDefinitionMetaV1();
		meta.setCode(code);
		meta.setGroup(group);
		meta.setLabel(label);
		meta.setDescription(description);
		return meta;
	}
}
