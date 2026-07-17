package com.aidigital.aionboarding.service.permission.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PermissionDefaultsProvider {

    public Map<String, Boolean> baseDefaults(String roleCode) {
        if (UserRoleCode.ADMIN.equals(roleCode)) {
            return PermissionKeys.ALL.stream().collect(Collectors.toMap(k -> k, k -> true));
        }
        if (UserRoleCode.TEAMLEAD.equals(roleCode)) {
            return teamLeadDefaults();
        }
        return memberDefaults();
    }

    Map<String, Boolean> teamLeadDefaults() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put(PermissionKeys.ADMIN_MANAGE_ROLES, false);
        map.put(PermissionKeys.PERMISSIONS_MANAGE_TEAMLEADS, false);
        map.put(PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS, true);
        map.put(PermissionKeys.TEAMS_MANAGE_MEMBERS, true);
        map.put(PermissionKeys.GROUPS_MANAGE, false);
        map.put(PermissionKeys.GRADES_MANAGE, true);
        map.put(PermissionKeys.MATERIALS_CREATE, true);
        map.put(PermissionKeys.MATERIALS_EDIT, true);
        map.put(PermissionKeys.MATERIALS_DELETE, true);
        map.put(PermissionKeys.LESSONS_CREATE, true);
        map.put(PermissionKeys.LESSONS_MANAGE, true);
        map.put(PermissionKeys.LESSONS_PUBLISH_ARCHIVE, true);
        map.put(PermissionKeys.LESSONS_MANAGE_ACTIVITIES, true);
        map.put(PermissionKeys.LESSONS_MANAGE_ASSETS, true);
        map.put(PermissionKeys.ROADMAPS_CREATE, true);
        map.put(PermissionKeys.ROADMAPS_MANAGE, true);
        map.put(PermissionKeys.LEARNING_ENROLL, true);
        map.put(PermissionKeys.LEARNING_ASSIGN, true);
        map.put(PermissionKeys.LEARNING_COMPLETE, true);
        map.put(PermissionKeys.LEARNING_ASK, true);
        return map;
    }

    Map<String, Boolean> memberDefaults() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put(PermissionKeys.ADMIN_MANAGE_ROLES, false);
        map.put(PermissionKeys.PERMISSIONS_MANAGE_TEAMLEADS, false);
        map.put(PermissionKeys.PERMISSIONS_MANAGE_TEAM_MEMBERS, false);
        map.put(PermissionKeys.TEAMS_MANAGE_MEMBERS, false);
        map.put(PermissionKeys.GROUPS_MANAGE, false);
        map.put(PermissionKeys.GRADES_MANAGE, false);
        map.put(PermissionKeys.MATERIALS_CREATE, false);
        map.put(PermissionKeys.MATERIALS_EDIT, false);
        map.put(PermissionKeys.MATERIALS_DELETE, false);
        map.put(PermissionKeys.LESSONS_CREATE, false);
        map.put(PermissionKeys.LESSONS_MANAGE, false);
        map.put(PermissionKeys.LESSONS_PUBLISH_ARCHIVE, false);
        map.put(PermissionKeys.LESSONS_MANAGE_ACTIVITIES, false);
        map.put(PermissionKeys.LESSONS_MANAGE_ASSETS, false);
        map.put(PermissionKeys.ROADMAPS_CREATE, false);
        map.put(PermissionKeys.ROADMAPS_MANAGE, false);
        map.put(PermissionKeys.LEARNING_ENROLL, true);
        map.put(PermissionKeys.LEARNING_ASSIGN, false);
        map.put(PermissionKeys.LEARNING_COMPLETE, true);
        map.put(PermissionKeys.LEARNING_ASK, true);
        return map;
    }
}
