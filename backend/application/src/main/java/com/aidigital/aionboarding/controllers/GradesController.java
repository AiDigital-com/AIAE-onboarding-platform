package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.GradesApi;
import com.aidigital.aionboarding.api.v1.model.CreateGradeRequestV1;
import com.aidigital.aionboarding.api.v1.model.GradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.GradesListResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.UpdateGradeRequestV1;
import com.aidigital.aionboarding.mappers.grade.GradeApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.grade.models.CreateGradeInput;
import com.aidigital.aionboarding.service.grade.models.UpdateGradeInput;
import com.aidigital.aionboarding.service.grade.services.GradeService;
import com.aidigital.aionboarding.support.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradesController implements GradesApi {

    private final CurrentUserSupport currentUser;
    private final GradeService gradeService;
    private final GradeApiMapper gradeApiMapper;
    private final ApiResponses apiResponses;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GradesListResponseV1> listGrades(Boolean includeInactive) {
        AppUser viewer = currentUser.requireUser();
        var grades = Boolean.TRUE.equals(includeInactive) ? gradeService.listAll(viewer) : gradeService.listActive();
        return ResponseEntity.ok(gradeApiMapper.toGradesListResponseV1(grades));
    }

    @Override
    @Transactional
    public ResponseEntity<GradeResponseV1> createGrade(CreateGradeRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(gradeApiMapper.toGradeResponseV1(
            gradeService.create(viewer, new CreateGradeInput(request.getName()))
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<GradeResponseV1> updateGrade(Long id, UpdateGradeRequestV1 request) {
        AppUser viewer = currentUser.requireUser();
        return ResponseEntity.ok(gradeApiMapper.toGradeResponseV1(
            gradeService.update(viewer, id, new UpdateGradeInput(request.getName()))
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<OkResponseV1> deactivateGrade(Long id) {
        AppUser viewer = currentUser.requireUser();
        gradeService.deactivate(viewer, id);
        return ResponseEntity.ok(apiResponses.ok());
    }

    @Override
    @Transactional
    public ResponseEntity<OkResponseV1> activateGrade(Long id) {
        AppUser viewer = currentUser.requireUser();
        gradeService.activate(viewer, id);
        return ResponseEntity.ok(apiResponses.ok());
    }
}
