package com.aidigital.aionboarding.service.user.support;

import com.aidigital.aionboarding.domain.common.dictionary.UserRoleCode;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.user.entities.User;
import com.aidigital.aionboarding.service.common.dictionary.DictionaryLookupService;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.user.services.UserFirstLoginAssignmentSyncService;
import com.aidigital.aionboarding.service.user.services.entity.UserEntityService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Creates the user row for a first-time login and resolves role references, for
 * {@code UserServiceImpl}. Owns the dictionary lookups so the orchestrator stays within its
 * dependency budget.
 */
@Component
@RequiredArgsConstructor
public class UserProvisioningSupport {

    private final UserEntityService userEntityService;
    private final DictionaryLookupService dictionaryLookupService;
    private final UserFirstLoginAssignmentSyncService userFirstLoginAssignmentSyncService;
    private final CurrentTime currentTime;

    /**
     * Persists a brand-new Member for a Clerk identity seen for the first time and hands the new
     * user to the standing "auto-assign to new users" roadmap rule in the same transaction, so a
     * new hire either exists with their roadmaps or not at all.
     *
     * @param clerkUserId Clerk subject of the authenticated identity
     * @param email       normalized email address
     * @param fullName    display name from Clerk; falls back to the email when blank
     * @return the saved user
     */
    public User createUser(String clerkUserId, String email, String fullName) {
        UserRole memberRole = dictionaryLookupService.getUserRoleReference(UserRoleCode.MEMBER);
        LocalDateTime now = currentTime.utcDateTime();
        User user = new User();
        user.setClerkUserId(clerkUserId);
        user.setEmail(email);
        user.setName(fullName == null || fullName.isBlank() ? email : fullName.trim());
        user.setRole(memberRole);
        user.setPosition("");
        user.setAvatarStorageKey("");
        user.setAvatarColor("");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        User saved = userEntityService.save(user);
        userFirstLoginAssignmentSyncService.onUserCreated(saved.getId());
        return saved;
    }

    /**
     * Resolves a role dictionary reference by code.
     *
     * @param roleCode role code such as {@code admin}, {@code teamlead}, or {@code member}
     * @return the role reference
     */
    public UserRole resolveRole(String roleCode) {
        return dictionaryLookupService.getUserRoleReference(roleCode);
    }
}
