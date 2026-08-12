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
import com.aidigital.aionboarding.support.PaginationSupport;
import lombok.RequiredArgsConstructor;
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
    private final PaginationSupport paginationSupport;
    private final UserApiMapper userApiMapper;
    private final LearningApiMapper learningApiMapper;

    @Override
    @PreAuthorize("@perm.has('" + PermissionKeys.LEARNING_ASSIGN + "')")
    @Transactional(readOnly = true)
    public ResponseEntity<UsersListResponseV1> listAssignableUsers(Integer page, Integer size, String query) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(userApiMapper.toUsersListResponseV1(
            userService.listAssignableUsers(viewer, query, paginationSupport.sortedByNameEmail(page, size))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<MyLessonsResponseV1> getMyLessons(Integer page, Integer size) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(learningApiMapper.toMyLessonsResponseV1(
            learningEnrollmentService.getMyLessons(viewer, paginationSupport.unsortedPageable(page, size))
        ));
    }
}
