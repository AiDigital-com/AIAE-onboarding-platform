package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.model.MyLessonsResponseV1;
import com.aidigital.aionboarding.mappers.learning.LearningApiMapper;
import com.aidigital.aionboarding.mappers.user.UserApiMapper;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.learning.models.MyLessonSummaryRecord;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningControllerTest {

	@Mock
	private CurrentUserSupport currentUser;
	@Mock
	private UserService userService;
	@Mock
	private LearningEnrollmentService learningEnrollmentService;
	@Mock
	private UserApiMapper userApiMapper;
	@Mock
	private LearningApiMapper learningApiMapper;

	@InjectMocks
	private LearningController controller;

	@Test
	void getMyLessonsShouldUseAnUnsortedPageRequestNotThePropertySortedOneTest() {
		// Given: the repository query hardcodes its own incomplete-first/newest-enrolled ORDER
		// BY, so the controller must not apply the name/email property sort used elsewhere
		AppUser viewer = new AppUser(1L, "clerk-1", "learner@test.com", "Learner", "member", "Learner", null, null,
				null);
		Page<MyLessonSummaryRecord> page = new PageImpl<>(List.of());
		MyLessonsResponseV1 expectedBody = mock(MyLessonsResponseV1.class);
		when(currentUser.requireUser()).thenReturn(viewer);
		when(learningEnrollmentService.getMyLessons(org.mockito.ArgumentMatchers.eq(viewer),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(page);
		when(learningApiMapper.toMyLessonsResponseV1(page)).thenReturn(expectedBody);

		// When:
		ResponseEntity<MyLessonsResponseV1> response = controller.getMyLessons(2, 15);

		// Then:
		assertThat(response.getBody()).isSameAs(expectedBody);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(learningEnrollmentService).getMyLessons(org.mockito.ArgumentMatchers.eq(viewer),
				pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(15);
		assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
	}

	@Test
	void unsortedPageableShouldClampNullAndOutOfRangeInputsTest() {
		// When / Then: negative/null page normalizes to 0, size clamps into [1, 100]
		assertThat(controller.unsortedPageable(null, null).getPageNumber()).isEqualTo(0);
		assertThat(controller.unsortedPageable(null, null).getPageSize()).isEqualTo(20);
		assertThat(controller.unsortedPageable(-5, 500).getPageNumber()).isEqualTo(0);
		assertThat(controller.unsortedPageable(-5, 500).getPageSize()).isEqualTo(100);
		assertThat(controller.unsortedPageable(3, 0).getPageSize()).isEqualTo(1);
	}
}
