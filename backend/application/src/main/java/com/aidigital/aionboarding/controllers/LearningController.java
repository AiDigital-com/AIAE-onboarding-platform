package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.LearningApi;
import com.aidigital.aionboarding.api.v1.model.MyLessonsResponseV1;
import com.aidigital.aionboarding.api.v1.model.UsersListResponseV1;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.permission.PermissionKeys;
import com.aidigital.aionboarding.service.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LearningController implements LearningApi {

    private final CurrentUserSupport currentUser;
    private final UserService userService;
    private final LearningEnrollmentService learningEnrollmentService;
    private final UserApiMapper userApiMapper;
    private final LearningApiMapper learningApiMapper;

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
    @Transactional(readOnly = true)
    public ResponseEntity<UsersListResponseV1> listAssignableUsers(Integer page, Integer size, String query) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(userApiMapper.toUsersListResponseV1(
            userService.listAssignableUsers(viewer, query, pageable(page, size))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<MyLessonsResponseV1> getMyLessons(Integer page, Integer size) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(learningApiMapper.toMyLessonsResponseV1(
            learningEnrollmentService.getMyLessons(viewer, unsortedPageable(page, size))
        ));
    }

    Pageable pageable(Integer page, Integer size) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(
            Sort.Order.asc("name").ignoreCase(),
            Sort.Order.asc("email").ignoreCase()
        ));
    }

    /**
     * Builds an unsorted page request for queries that hardcode their own {@code ORDER BY}
     * (e.g. incomplete-first, newest-enrolled-first for My Lessons) rather than a property sort.
     *
     * @param page requested zero-based page index
     * @param size requested page size
     * @return a bounded, unsorted page request
     */
    Pageable unsortedPageable(Integer page, Integer size) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        return PageRequest.of(normalizedPage, normalizedSize);
    }
}
