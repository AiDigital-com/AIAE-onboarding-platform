package com.aidigital.aionboarding.security;

import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("perm")
@RequiredArgsConstructor
public class PermissionEvaluator {

    private final AppUserFactory appUserFactory;
    private final PermissionService permissionService;

    public boolean has(String permissionKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        AppUser user = appUserFactory.from(auth);
        return permissionService.userHasPermission(user, permissionKey);
    }
}
