package com.aidigital.aionboarding.service.common.dictionary;

import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityProgressStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.ActivityType;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonAssetKind;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonContentFormat;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonPublicationStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.LessonStatus;
import com.aidigital.aionboarding.domain.common.dictionary.entities.MaterialFileKind;
import com.aidigital.aionboarding.domain.common.dictionary.entities.UserRole;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.ActivityProgressStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.ActivityTypeRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonAssetKindRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonContentFormatRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonPublicationStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.LessonStatusRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.MaterialFileKindRepository;
import com.aidigital.aionboarding.domain.common.dictionary.repositories.UserRoleRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryLookupServiceTest {

	@Mock
	private UserRoleRepository userRoleRepository;
	@Mock
	private LessonStatusRepository lessonStatusRepository;
	@Mock
	private LessonPublicationStatusRepository lessonPublicationStatusRepository;
	@Mock
	private LessonContentFormatRepository lessonContentFormatRepository;
	@Mock
	private ActivityTypeRepository activityTypeRepository;
	@Mock
	private ActivityProgressStatusRepository activityProgressStatusRepository;
	@Mock
	private LessonAssetKindRepository lessonAssetKindRepository;
	@Mock
	private MaterialFileKindRepository materialFileKindRepository;

	@InjectMocks
	private DictionaryLookupService service;

	@Test
	void shouldLookupUserRoleIdTest() {
		// Given:
		UserRole role = new UserRole();
		role.setId(1L);
		when(userRoleRepository.findByCode("admin")).thenReturn(Optional.of(role));

		// When:
		Long result = service.userRoleId("admin");

		// Then:
		assertThat(result).isEqualTo(1L);
	}

	@Test
	void shouldCacheRepeatedLookupsTest() {
		// Given:
		UserRole role = new UserRole();
		role.setId(2L);
		when(userRoleRepository.findByCode("member")).thenReturn(Optional.of(role));

		// When:
		Long first = service.userRoleId("member");
		Long second = service.userRoleId("member");

		// Then:
		assertThat(first).isEqualTo(second);
		verify(userRoleRepository, times(1)).findByCode("member");
	}

	@Test
	void shouldLookupLessonStatusIdTest() {
		// Given:
		LessonStatus status = new LessonStatus();
		status.setId(3L);
		when(lessonStatusRepository.findByCode("ready")).thenReturn(Optional.of(status));

		// When:
		Long result = service.lessonStatusId("ready");

		// Then:
		assertThat(result).isEqualTo(3L);
	}

	@Test
	void shouldLookupLessonPublicationStatusIdTest() {
		// Given:
		LessonPublicationStatus status = new LessonPublicationStatus();
		status.setId(4L);
		when(lessonPublicationStatusRepository.findByCode("published")).thenReturn(Optional.of(status));

		// When:
		Long result = service.lessonPublicationStatusId("published");

		// Then:
		assertThat(result).isEqualTo(4L);
	}

	@Test
	void shouldLookupLessonContentFormatIdTest() {
		// Given:
		LessonContentFormat format = new LessonContentFormat();
		format.setId(5L);
		when(lessonContentFormatRepository.findByCode("markdown")).thenReturn(Optional.of(format));

		// When:
		Long result = service.lessonContentFormatId("markdown");

		// Then:
		assertThat(result).isEqualTo(5L);
	}

	@Test
	void shouldLookupActivityTypeIdTest() {
		// Given:
		ActivityType type = new ActivityType();
		type.setId(6L);
		when(activityTypeRepository.findByCode("quiz")).thenReturn(Optional.of(type));

		// When:
		Long result = service.activityTypeId("quiz");

		// Then:
		assertThat(result).isEqualTo(6L);
	}

	@Test
	void shouldLookupActivityProgressStatusIdTest() {
		// Given:
		ActivityProgressStatus status = new ActivityProgressStatus();
		status.setId(7L);
		when(activityProgressStatusRepository.findByCode("completed")).thenReturn(Optional.of(status));

		// When:
		Long result = service.activityProgressStatusId("completed");

		// Then:
		assertThat(result).isEqualTo(7L);
	}

	@Test
	void shouldLookupLessonAssetKindIdTest() {
		// Given:
		LessonAssetKind kind = new LessonAssetKind();
		kind.setId(8L);
		when(lessonAssetKindRepository.findByCode("link")).thenReturn(Optional.of(kind));

		// When:
		Long result = service.lessonAssetKindId("link");

		// Then:
		assertThat(result).isEqualTo(8L);
	}

	@Test
	void shouldLookupMaterialFileKindIdTest() {
		// Given:
		MaterialFileKind kind = new MaterialFileKind();
		kind.setId(9L);
		when(materialFileKindRepository.findByCode("file")).thenReturn(Optional.of(kind));

		// When:
		Long result = service.materialFileKindId("file");

		// Then:
		assertThat(result).isEqualTo(9L);
	}

	@Test
	void shouldThrowWhenLookupCodeNotFoundTest() {
		// Given:
		when(userRoleRepository.findByCode("missing")).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> service.userRoleId("missing"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("dictionary:missing");
	}

	@Test
	void shouldReturnUserRoleReferenceTest() {
		// Given:
		UserRole role = new UserRole();
		role.setId(1L);
		when(userRoleRepository.findByCode("admin")).thenReturn(Optional.of(role));

		// When:
		UserRole result = service.getUserRoleReference("admin");

		// Then:
		assertThat(result).isSameAs(role);
	}

	@Test
	void shouldReturnActivityTypeReferenceTest() {
		// Given:
		ActivityType type = new ActivityType();
		type.setId(6L);
		when(activityTypeRepository.findByCode("quiz")).thenReturn(Optional.of(type));

		// When:
		ActivityType result = service.getActivityTypeReference("quiz");

		// Then:
		assertThat(result).isSameAs(type);
	}

	@Test
	void shouldReturnActivityProgressStatusReferenceTest() {
		// Given:
		ActivityProgressStatus status = new ActivityProgressStatus();
		status.setId(7L);
		when(activityProgressStatusRepository.findByCode("completed")).thenReturn(Optional.of(status));

		// When:
		ActivityProgressStatus result = service.getActivityProgressStatusReference("completed");

		// Then:
		assertThat(result).isSameAs(status);
	}

	@Test
	void shouldReturnLessonAssetKindReferenceTest() {
		// Given:
		LessonAssetKind kind = new LessonAssetKind();
		kind.setId(8L);
		when(lessonAssetKindRepository.findByCode("link")).thenReturn(Optional.of(kind));

		// When:
		LessonAssetKind result = service.getLessonAssetKindReference("link");

		// Then:
		assertThat(result).isSameAs(kind);
	}

	@Test
	void shouldThrowWhenReferenceNotFoundTest() {
		// Given:
		when(userRoleRepository.findByCode("ghost")).thenReturn(Optional.empty());

		// When-Then:
		assertThatThrownBy(() -> service.getUserRoleReference("ghost"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("user_role:ghost");
	}
}
