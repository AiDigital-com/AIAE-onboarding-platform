package com.aidigital.aionboarding.service.material.services.impl;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.domain.material.repositories.MaterialRepository;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.security.AppUser;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialListQuery;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSortField;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import com.aidigital.aionboarding.service.material.services.MaterialFileService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService;
import com.aidigital.aionboarding.service.material.services.MaterialLinkService.PreparedLinkRecord;
import com.aidigital.aionboarding.service.material.services.MaterialRecordQueryService;
import com.aidigital.aionboarding.service.material.services.MaterialYoutubeService;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator;
import com.aidigital.aionboarding.service.material.validation.MaterialPayloadValidator.ValidatedMaterialPayload;
import com.aidigital.aionboarding.service.permission.services.PermissionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceImplTest {

	@Mock
	private MaterialRepository materialRepository;
	@Mock
	private PermissionService permissionService;
	@Mock
	private MaterialRecordQueryService materialRecordQueryService;
	@Mock
	private MaterialPayloadValidator materialPayloadValidator;
	@Mock
	private MaterialYoutubeService materialYoutubeService;
	@Mock
	private MaterialLinkService materialLinkService;
	@Mock
	private MaterialFileService materialFileService;
	@Mock
	private MaterialPersistenceService materialPersistenceService;

	@InjectMocks
	private MaterialServiceImpl service;

	@Nested
	class GetAllTests {

		@Test
		void getAllShouldDelegateToRecordQueryServiceAndReturnItsPageTest() {
			// Given:
			AppUser viewer = appUser(1L);
			MaterialListQuery query = new MaterialListQuery(
					"term", null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.DESC
			);
			MaterialSearchSummaryRecord record = org.mockito.Mockito.mock(MaterialSearchSummaryRecord.class);
			Page<MaterialSearchSummaryRecord> page = new PageImpl<>(List.of(record));
			when(materialRecordQueryService.loadMaterialSummaries(query, 0, 20)).thenReturn(page);

			// When:
			Page<MaterialSearchSummaryRecord> result = service.getAll(viewer, query, 0, 20);

			// Then:
			assertThat(result).isSameAs(page);
			verify(materialRecordQueryService).loadMaterialSummaries(query, 0, 20);
		}
	}

	@Nested
	class CountTests {

		@Test
		void countShouldDelegateToRecordQueryServiceTest() {
			// Given:
			AppUser viewer = appUser(1L);
			MaterialListQuery query = new MaterialListQuery(
					"term", null, null, null, null, null, MaterialSortField.CREATED_AT, Sort.Direction.DESC
			);
			when(materialRecordQueryService.countSummaries(query)).thenReturn(7L);

			// When:
			long result = service.count(viewer, query);

			// Then:
			assertThat(result).isEqualTo(7L);
			verify(materialRecordQueryService).countSummaries(query);
		}
	}

	@Nested
	class GetByIdsTests {

		@Test
		void getByIdsShouldReorderResultsToMatchRequestedIdOrderTest() {
			// Given:
			MaterialRecord first = new MaterialRecord(
					1L, "First", "d", "t", "", "", "", null, "creator", List.of(), null, null, 0L,
					List.of(), List.of(), List.of(), List.of(), List.of()
			);
			MaterialRecord second = new MaterialRecord(
					2L, "Second", "d", "t", "", "", "", null, "creator", List.of(), null, null, 0L,
					List.of(), List.of(), List.of(), List.of(), List.of()
			);
			when(materialRecordQueryService.loadMaterialRecordsByIds(List.of(2L, 1L)))
					.thenReturn(List.of(first, second));

			// When:
			List<MaterialRecord> result = service.getByIds(List.of(2L, 1L));

			// Then:
			assertThat(result).containsExactly(second, first);
		}

		@Test
		void getByIdsShouldReturnEmptyListWithoutQueryingWhenNoIdsGivenTest() {
			// When:
			List<MaterialRecord> result = service.getByIds(List.of());

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class GetByIdTests {

		@Test
		void getByIdShouldAssembleFullRecordFromEntityAndUsageCountTest() {
			// Given:
			Material mat = material(10L);
			MaterialRecord expected = org.mockito.Mockito.mock(MaterialRecord.class);
			when(materialRepository.findById(10L)).thenReturn(Optional.of(mat));
			when(materialRecordQueryService.countLessonUsage(10L)).thenReturn(5L);
			when(materialRecordQueryService.loadRecord(mat, 5L)).thenReturn(expected);

			// When:
			MaterialRecord result = service.getById(10L);

			// Then:
			assertThat(result).isSameAs(expected);
		}

		@Test
		void getByIdShouldThrowC001WhenMaterialMissingTest() {
			// Given:
			when(materialRepository.findById(99L)).thenReturn(Optional.empty());

			// When-Then:
			org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getById(99L))
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C001.name()));
		}
	}

	@Nested
	class CreateTests {

		@Test
		void prepareLinksIsCalledBeforeCreatePersistence() {
			// Given
			AppUser viewer = appUser(1L);
			CreateMaterialInput input = createInput(List.of("https://example.com"), List.of());
			ValidatedMaterialPayload payload = payload(List.of("https://example.com"), List.of());
			PreparedLinkRecord linkRecord = linkRecord("https://example.com");
			MaterialRecord materialRecord = mock(MaterialRecord.class);

			when(materialPayloadValidator.validateCreateInput(input)).thenReturn(payload);
			when(materialLinkService.prepareLinks(List.of("https://example.com")))
					.thenReturn(List.of(linkRecord));
			when(materialYoutubeService.prepareYoutubeMetadata(List.of()))
					.thenReturn(List.of());
			when(materialPersistenceService.create(viewer, input, payload, List.of(linkRecord), List.of()))
					.thenReturn(materialRecord);

			// Execution
			MaterialRecord result = service.create(viewer, input);

			// Verification
			assertThat(result).isSameAs(materialRecord);
			verify(materialLinkService).prepareLinks(List.of("https://example.com"));
			verify(materialPersistenceService).create(viewer, input, payload, List.of(linkRecord), List.of());
		}

		@Test
		void createPersistenceReceivesPreparedLinkRecordList() {
			// Given
			AppUser viewer = appUser(2L);
			PreparedLinkRecord linkRecord = linkRecord("https://example.com");
			CreateMaterialInput input = createInput(List.of("https://example.com"), List.of());
			ValidatedMaterialPayload payload = payload(List.of("https://example.com"), List.of());
			MaterialRecord materialRecord = mock(MaterialRecord.class);

			when(materialPayloadValidator.validateCreateInput(input)).thenReturn(payload);
			when(materialLinkService.prepareLinks(anyList())).thenReturn(List.of(linkRecord));
			when(materialYoutubeService.prepareYoutubeMetadata(anyList())).thenReturn(List.of());
			when(materialPersistenceService.create(eq(viewer), eq(input), eq(payload), anyList(), anyList()))
					.thenReturn(materialRecord);

			// Execution
			service.create(viewer, input);

			// Verification
			ArgumentCaptor<List<PreparedLinkRecord>> captor =
					ArgumentCaptor.forClass(List.class);
			verify(materialPersistenceService).create(eq(viewer), eq(input), eq(payload), captor.capture(),
					eq(List.of()));
			assertThat(captor.getValue()).containsExactly(linkRecord);
		}
	}

	@Nested
	class DeleteTests {

		@Test
		void deleteRegistersAfterCommitSynchronizationThatCallsDeleteStorageKeys() {
			// Given
			AppUser viewer = appUser(3L);
			Material mat = material(10L);
			List<String> storageKeys = List.of("key1", "key2");

			when(materialRepository.findById(10L)).thenReturn(Optional.of(mat));
			when(materialFileService.collectStorageKeys(10L)).thenReturn(storageKeys);
			doNothing().when(materialRecordQueryService).requireDeletable(10L);

			try (MockedStatic<TransactionSynchronizationManager> txMgr =
						 mockStatic(TransactionSynchronizationManager.class)) {
				txMgr.when(TransactionSynchronizationManager::isSynchronizationActive)
						.thenReturn(true);

				ArgumentCaptor<TransactionSynchronization> syncCaptor =
						ArgumentCaptor.forClass(TransactionSynchronization.class);

				// Execution
				service.delete(viewer, 10L);

				// Verification — synchronization was registered
				txMgr.verify(() ->
						TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture())
				);

				// Invoke the afterCommit callback manually
				syncCaptor.getValue().afterCommit();
				verify(materialFileService).deleteStorageKeysQuietly(storageKeys);
			}
		}

		@Test
		void afterCommitCallbackSwallowsRuntimeException() {
			// Given
			AppUser viewer = appUser(4L);
			Material mat = material(20L);
			List<String> storageKeys = List.of("key3");

			when(materialRepository.findById(20L)).thenReturn(Optional.of(mat));
			when(materialFileService.collectStorageKeys(20L)).thenReturn(storageKeys);
			doNothing().when(materialRecordQueryService).requireDeletable(20L);
			doThrow(new RuntimeException("S3 unavailable"))
					.when(materialFileService).deleteStorageKeysQuietly(anyList());

			try (MockedStatic<TransactionSynchronizationManager> txMgr =
						 mockStatic(TransactionSynchronizationManager.class)) {
				txMgr.when(TransactionSynchronizationManager::isSynchronizationActive)
						.thenReturn(true);

				ArgumentCaptor<TransactionSynchronization> syncCaptor =
						ArgumentCaptor.forClass(TransactionSynchronization.class);

				service.delete(viewer, 20L);

				txMgr.verify(() ->
						TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture())
				);

				// Execution — call afterCommit with deleteStorageKeysQuietly throwing
				// Verification — no exception propagates
				assertThatNoException().isThrownBy(() -> syncCaptor.getValue().afterCommit());
			}
		}
	}

	@Nested
	class UpdateTests {

		@Test
		void updateDelegatesPreparedDataToPersistenceService() {
			// Given
			AppUser viewer = appUser(5L);
			Long materialId = 30L;
			UpdateMaterialInput input = updateInput(List.of(), List.of());
			ValidatedMaterialPayload payload = payload(List.of(), List.of());
			List<String> existingKeys = List.of("old-key");
			MaterialRecord materialRecord = mock(MaterialRecord.class);

			when(materialPayloadValidator.validateUpdateInput(input)).thenReturn(payload);
			when(materialFileService.collectStorageKeys(materialId)).thenReturn(existingKeys);
			when(materialLinkService.prepareLinks(anyList())).thenReturn(List.of());
			when(materialYoutubeService.prepareYoutubeMetadata(anyList())).thenReturn(List.of());
			when(materialPersistenceService.update(viewer, materialId, input, payload, List.of(), List.of(),
					existingKeys))
					.thenReturn(materialRecord);

			// Execution
			MaterialRecord result = service.update(viewer, materialId, input);

			// Verification
			assertThat(result).isSameAs(materialRecord);
			verify(materialPersistenceService).update(viewer, materialId, input, payload, List.of(), List.of(),
					existingKeys);
		}
	}

	// ─── Helpers ────────────────────────────────────────────────────────────────

	private AppUser appUser(Long id) {
		return new AppUser(id, "clerk-" + id, "user@example.com", "User " + id,
				"admin", "User", null, null, null);
	}

	private Material material(Long id) {
		Material m = new Material();
		m.setId(id);
		return m;
	}

	private MaterialRecord mock(Class<MaterialRecord> clazz) {
		return org.mockito.Mockito.mock(clazz);
	}

	private PreparedLinkRecord linkRecord(String url) {
		return new PreparedLinkRecord(url, "Title", "Desc", "", "", "", "");
	}

	private ValidatedMaterialPayload payload(List<String> links, List<String> youtubeUrls) {
		return new ValidatedMaterialPayload(
				"Test title", "desc", "text",
				youtubeUrls, links, List.of(), List.of()
		);
	}

	private CreateMaterialInput createInput(List<String> links, List<String> youtubeUrls) {
		return new CreateMaterialInput(
				"Test title", "desc", "text",
				youtubeUrls, links, List.of(), List.of(),
				null, null, null
		);
	}

	private UpdateMaterialInput updateInput(List<String> links, List<String> youtubeUrls) {
		return new UpdateMaterialInput(
				"Test title", "desc", "text",
				youtubeUrls, links, List.of(), List.of(),
				null, null, null
		);
	}
}
