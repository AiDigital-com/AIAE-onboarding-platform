package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.CreateGradeRequestV1;
import com.aidigital.aionboarding.api.v1.model.GradeResponseV1;
import com.aidigital.aionboarding.api.v1.model.GradesListResponseV1;
import com.aidigital.aionboarding.api.v1.model.OkResponseV1;
import com.aidigital.aionboarding.api.v1.model.UpdateGradeRequestV1;
import com.aidigital.aionboarding.mappers.grade.GradeApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.grade.models.CreateGradeInput;
import com.aidigital.aionboarding.service.grade.models.GradeRecord;
import com.aidigital.aionboarding.service.grade.models.UpdateGradeInput;
import com.aidigital.aionboarding.service.grade.services.GradeService;
import com.aidigital.aionboarding.support.ApiResponses;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradesControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private GradeService gradeService;
	@Mock
	private GradeApiMapper gradeApiMapper;
	@Mock
	private ApiResponses apiResponses;

	@InjectMocks
	private GradesController controller;

	@Test
	void shouldListActiveGradesWhenIncludeInactiveIsFalseTest() {
		// Given:
		AppUser viewer = viewer();
		List<GradeRecord> grades = Instancio.ofList(GradeRecord.class).create();
		GradesListResponseV1 expectedBody = Instancio.create(GradesListResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(gradeService.listActive()).thenReturn(grades);
		when(gradeApiMapper.toGradesListResponseV1(grades)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GradesListResponseV1> response = controller.listGrades(false);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldListAllGradesWhenIncludeInactiveIsTrueTest() {
		// Given:
		AppUser viewer = viewer();
		List<GradeRecord> grades = Instancio.ofList(GradeRecord.class).create();
		GradesListResponseV1 expectedBody = Instancio.create(GradesListResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(gradeService.listAll(viewer)).thenReturn(grades);
		when(gradeApiMapper.toGradesListResponseV1(grades)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GradesListResponseV1> response = controller.listGrades(true);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldCreateGradeTest() {
		// Given:
		AppUser viewer = viewer();
		CreateGradeRequestV1 request = new CreateGradeRequestV1().name("Senior");
		GradeRecord created = Instancio.create(GradeRecord.class);
		GradeResponseV1 expectedBody = Instancio.create(GradeResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(gradeService.create(viewer, new CreateGradeInput("Senior"))).thenReturn(created);
		when(gradeApiMapper.toGradeResponseV1(created)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GradeResponseV1> response = controller.createGrade(request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldUpdateGradeTest() {
		// Given:
		AppUser viewer = viewer();
		UpdateGradeRequestV1 request = new UpdateGradeRequestV1().name("Lead");
		GradeRecord updated = Instancio.create(GradeRecord.class);
		GradeResponseV1 expectedBody = Instancio.create(GradeResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(gradeService.update(viewer, 5L, new UpdateGradeInput("Lead"))).thenReturn(updated);
		when(gradeApiMapper.toGradeResponseV1(updated)).thenReturn(expectedBody);

		// When:
		ResponseEntity<GradeResponseV1> response = controller.updateGrade(5L, request);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
	}

	@Test
	void shouldDeactivateGradeTest() {
		// Given:
		AppUser viewer = viewer();
		OkResponseV1 expectedBody = Instancio.create(OkResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(apiResponses.ok()).thenReturn(expectedBody);

		// When:
		ResponseEntity<OkResponseV1> response = controller.deactivateGrade(5L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(gradeService).deactivate(viewer, 5L);
	}

	@Test
	void shouldActivateGradeTest() {
		// Given:
		AppUser viewer = viewer();
		OkResponseV1 expectedBody = Instancio.create(OkResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(apiResponses.ok()).thenReturn(expectedBody);

		// When:
		ResponseEntity<OkResponseV1> response = controller.activateGrade(5L);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		verify(gradeService).activate(viewer, 5L);
	}

	AppUser viewer() {
		return new AppUser(1L, "clerk-1", "user@test.com", "User", "admin", "User", null, null, null);
	}
}
