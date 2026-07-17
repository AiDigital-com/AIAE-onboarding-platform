package com.aidigital.aionboarding.service.permission.services.impl;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.services.PermissionPolicyService;
import org.springframework.stereotype.Service;

@Service
public class PermissionPolicyServiceImpl implements PermissionPolicyService {

	@Override
	public boolean isTeamManager(AppUser user) {
		return user != null && (user.isAdmin() || user.isTeamLead());
	}
}
