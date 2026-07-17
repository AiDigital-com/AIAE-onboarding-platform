package com.aidigital.aionboarding.service.permission;

import java.util.Set;

public final class PermissionKeys {

    public static final String ADMIN_MANAGE_ROLES = "admin.manage_roles";
    public static final String PERMISSIONS_MANAGE_TEAMLEADS = "permissions.manage_teamleads";
    public static final String PERMISSIONS_MANAGE_TEAM_MEMBERS = "permissions.manage_team_members";
    public static final String TEAMS_MANAGE_MEMBERS = "teams.manage_members";
    public static final String GROUPS_MANAGE = "groups.manage";
    public static final String GRADES_MANAGE = "grades.manage";
    public static final String MATERIALS_CREATE = "materials.create";
    public static final String MATERIALS_EDIT = "materials.edit";
    public static final String MATERIALS_DELETE = "materials.delete";
    public static final String LESSONS_CREATE = "lessons.create";
    public static final String LESSONS_MANAGE = "lessons.manage";
    public static final String LESSONS_PUBLISH_ARCHIVE = "lessons.publish_archive";
    public static final String LESSONS_MANAGE_ACTIVITIES = "lessons.manage_activities";
    public static final String LESSONS_MANAGE_ASSETS = "lessons.manage_assets";
    public static final String ROADMAPS_CREATE = "roadmaps.create";
    public static final String ROADMAPS_MANAGE = "roadmaps.manage";
    public static final String LEARNING_ENROLL = "learning.enroll";
    public static final String LEARNING_ASSIGN = "learning.assign";
    public static final String LEARNING_COMPLETE = "learning.complete";
    public static final String LEARNING_ASK = "learning.ask";

    public static final Set<String> ALL = Set.of(
        ADMIN_MANAGE_ROLES,
        PERMISSIONS_MANAGE_TEAMLEADS,
        PERMISSIONS_MANAGE_TEAM_MEMBERS,
        TEAMS_MANAGE_MEMBERS,
        GROUPS_MANAGE,
        GRADES_MANAGE,
        MATERIALS_CREATE,
        MATERIALS_EDIT,
        MATERIALS_DELETE,
        LESSONS_CREATE,
        LESSONS_MANAGE,
        LESSONS_PUBLISH_ARCHIVE,
        LESSONS_MANAGE_ACTIVITIES,
        LESSONS_MANAGE_ASSETS,
        ROADMAPS_CREATE,
        ROADMAPS_MANAGE,
        LEARNING_ENROLL,
        LEARNING_ASSIGN,
        LEARNING_COMPLETE,
        LEARNING_ASK
    );

    public static final Set<String> ADMIN_TO_TEAMLEAD_DENYLIST = Set.of(
        ADMIN_MANAGE_ROLES,
        PERMISSIONS_MANAGE_TEAMLEADS
    );

    public static final Set<String> TEAMLEAD_TO_MEMBER_DENYLIST = Set.of(
        ADMIN_MANAGE_ROLES,
        PERMISSIONS_MANAGE_TEAMLEADS,
        PERMISSIONS_MANAGE_TEAM_MEMBERS,
        TEAMS_MANAGE_MEMBERS,
        GROUPS_MANAGE,
        GRADES_MANAGE,
        LEARNING_ASSIGN
    );

    private PermissionKeys() {
    }
}
