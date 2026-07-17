package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.security.AppUserFactory;
import com.aidigital.aionboarding.service.common.security.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserSupport {

    private final AppUserFactory appUserFactory;

    public AppUser requireUser() {
        return appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
    }
}
