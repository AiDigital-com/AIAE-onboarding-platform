package com.aidigital.aionboarding.mappers.material;

import com.aidigital.aionboarding.api.v1.model.CreateMaterialRequestV1;
import com.aidigital.aionboarding.api.v1.model.CreateMaterialResponseV1;
import com.aidigital.aionboarding.api.v1.model.MaterialAssetKindV1;
import com.aidigital.aionboarding.api.v1.model.MaterialAttachmentInputV1;
import com.aidigital.aionboarding.api.v1.model.MaterialFileSummaryV1;
import com.aidigital.aionboarding.api.v1.model.MaterialFileV1;
import com.aidigital.aionboarding.api.v1.model.MaterialLinkAssetV1;
import com.aidigital.aionboarding.api.v1.model.MaterialLinkPreviewV1;
import com.aidigital.aionboarding.api.v1.model.MaterialResponseV1;
import com.aidigital.aionboarding.api.v1.model.MaterialSummaryV1;
import com.aidigital.aionboarding.api.v1.model.MaterialV1;
import com.aidigital.aionboarding.api.v1.model.MaterialYoutubeVideoV1;
import com.aidigital.aionboarding.api.v1.model.UpdateMaterialRequestV1;
import com.aidigital.aionboarding.api.v1.model.UploadedFileResponseV1;
import com.aidigital.aionboarding.mappers.common.MaterialAssetKindApiMapper;
import com.aidigital.aionboarding.service.material.models.CreateMaterialInput;
import com.aidigital.aionboarding.service.material.models.MaterialAttachmentInput;
import com.aidigital.aionboarding.service.material.models.MaterialFileRecord;
import com.aidigital.aionboarding.service.material.models.MaterialFileSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialLinkAssetRecord;
import com.aidigital.aionboarding.service.material.models.MaterialLinkSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialRecord;
import com.aidigital.aionboarding.service.material.models.MaterialSearchSummaryRecord;
import com.aidigital.aionboarding.service.material.models.MaterialYoutubeVideoRecord;
import com.aidigital.aionboarding.service.material.models.UpdateMaterialInput;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialApiMapperImplTest {

	@InjectMocks
	private MaterialApiMapperImpl materialApiMapperImpl;

	@Mock
	private MaterialAssetKindApiMapper materialAssetKindApiMapper;

	@BeforeEach
	void setUp() {
		when(materialAssetKindApiMapper.mapMaterialAssetKind(anyString())).thenReturn(Instancio.create(MaterialAssetKindV1.class));
		when(materialAssetKindApiMapper.fromMaterialAssetKind(any(MaterialAssetKindV1.class))).thenReturn("value");
	}

	@Test
	void shouldToMaterialYoutubeVideoV1MaterialYoutubeVideoRecordTest() {
		// Given:
		MaterialYoutubeVideoRecord video = Instancio.create(MaterialYoutubeVideoRecord.class);

		// When:
		MaterialYoutubeVideoV1 actualResult = materialApiMapperImpl.toMaterialYoutubeVideoV1(video);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialYoutubeVideoV1MaterialYoutubeVideoRecordWithNullTest() {
		// Given:
		MaterialYoutubeVideoRecord video = null;

		// When:
		MaterialYoutubeVideoV1 actualResult = materialApiMapperImpl.toMaterialYoutubeVideoV1(video);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialLinkAssetV1MaterialLinkAssetRecordTest() {
		// Given:
		MaterialLinkAssetRecord link = Instancio.create(MaterialLinkAssetRecord.class);

		// When:
		MaterialLinkAssetV1 actualResult = materialApiMapperImpl.toMaterialLinkAssetV1(link);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialLinkAssetV1MaterialLinkAssetRecordWithNullTest() {
		// Given:
		MaterialLinkAssetRecord link = null;

		// When:
		MaterialLinkAssetV1 actualResult = materialApiMapperImpl.toMaterialLinkAssetV1(link);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialFileV1MaterialFileRecordTest() {
		// Given:
		MaterialFileRecord file = Instancio.create(MaterialFileRecord.class);

		// When:
		MaterialFileV1 actualResult = materialApiMapperImpl.toMaterialFileV1(file);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialFileV1MaterialFileRecordWithNullTest() {
		// Given:
		MaterialFileRecord file = null;

		// When:
		MaterialFileV1 actualResult = materialApiMapperImpl.toMaterialFileV1(file);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialV1MaterialRecordTest() {
		// Given:
		MaterialRecord material = Instancio.create(MaterialRecord.class);

		// When:
		MaterialV1 actualResult = materialApiMapperImpl.toMaterialV1(material);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialV1MaterialRecordWithNullTest() {
		// Given:
		MaterialRecord material = null;

		// When:
		MaterialV1 actualResult = materialApiMapperImpl.toMaterialV1(material);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialResponseV1MaterialRecordTest() {
		// Given:
		MaterialRecord material = Instancio.create(MaterialRecord.class);

		// When:
		MaterialResponseV1 actualResult = materialApiMapperImpl.toMaterialResponseV1(material);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialResponseV1MaterialRecordWithNullTest() {
		// Given:
		MaterialRecord material = null;

		// When:
		MaterialResponseV1 actualResult = materialApiMapperImpl.toMaterialResponseV1(material);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialLinkPreviewV1MaterialLinkSummaryRecordTest() {
		// Given:
		MaterialLinkSummaryRecord link = Instancio.create(MaterialLinkSummaryRecord.class);

		// When:
		MaterialLinkPreviewV1 actualResult = materialApiMapperImpl.toMaterialLinkPreviewV1(link);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialLinkPreviewV1MaterialLinkSummaryRecordWithNullTest() {
		// Given:
		MaterialLinkSummaryRecord link = null;

		// When:
		MaterialLinkPreviewV1 actualResult = materialApiMapperImpl.toMaterialLinkPreviewV1(link);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialFileSummaryV1MaterialFileSummaryRecordTest() {
		// Given:
		MaterialFileSummaryRecord file = Instancio.create(MaterialFileSummaryRecord.class);

		// When:
		MaterialFileSummaryV1 actualResult = materialApiMapperImpl.toMaterialFileSummaryV1(file);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialFileSummaryV1MaterialFileSummaryRecordWithNullTest() {
		// Given:
		MaterialFileSummaryRecord file = null;

		// When:
		MaterialFileSummaryV1 actualResult = materialApiMapperImpl.toMaterialFileSummaryV1(file);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialSummaryV1MaterialSearchSummaryRecordTest() {
		// Given:
		MaterialSearchSummaryRecord material = Instancio.create(MaterialSearchSummaryRecord.class);

		// When:
		MaterialSummaryV1 actualResult = materialApiMapperImpl.toMaterialSummaryV1(material);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialSummaryV1MaterialSearchSummaryRecordWithNullTest() {
		// Given:
		MaterialSearchSummaryRecord material = null;

		// When:
		MaterialSummaryV1 actualResult = materialApiMapperImpl.toMaterialSummaryV1(material);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToCreateMaterialResponseV1MaterialRecordTest() {
		// Given:
		MaterialRecord material = Instancio.create(MaterialRecord.class);

		// When:
		CreateMaterialResponseV1 actualResult = materialApiMapperImpl.toCreateMaterialResponseV1(material);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCreateMaterialResponseV1MaterialRecordWithNullTest() {
		// Given:
		MaterialRecord material = null;

		// When:
		CreateMaterialResponseV1 actualResult = materialApiMapperImpl.toCreateMaterialResponseV1(material);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToUploadedFileResponseV1StringStringStringlongTest() {
		// Given:
		String storageKey = "value";
		String originalName = "value";
		String mimeType = "value";
		long sizeBytes = 5L;

		// When:
		UploadedFileResponseV1 actualResult = materialApiMapperImpl.toUploadedFileResponseV1(storageKey, originalName,
				mimeType, sizeBytes);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCreateMaterialInputCreateMaterialRequestV1Test() {
		// Given:
		CreateMaterialRequestV1 request = Instancio.create(CreateMaterialRequestV1.class);

		// When:
		CreateMaterialInput actualResult = materialApiMapperImpl.toCreateMaterialInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCreateMaterialInputCreateMaterialRequestV1WithNullTest() {
		// Given:
		CreateMaterialRequestV1 request = null;

		// When:
		CreateMaterialInput actualResult = materialApiMapperImpl.toCreateMaterialInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToUpdateMaterialInputUpdateMaterialRequestV1Test() {
		// Given:
		UpdateMaterialRequestV1 request = Instancio.create(UpdateMaterialRequestV1.class);

		// When:
		UpdateMaterialInput actualResult = materialApiMapperImpl.toUpdateMaterialInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUpdateMaterialInputUpdateMaterialRequestV1WithNullTest() {
		// Given:
		UpdateMaterialRequestV1 request = null;

		// When:
		UpdateMaterialInput actualResult = materialApiMapperImpl.toUpdateMaterialInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToMaterialAttachmentInputMaterialAttachmentInputV1Test() {
		// Given:
		MaterialAttachmentInputV1 attachment = Instancio.create(MaterialAttachmentInputV1.class);

		// When:
		MaterialAttachmentInput actualResult = materialApiMapperImpl.toMaterialAttachmentInput(attachment);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToMaterialAttachmentInputMaterialAttachmentInputV1WithNullTest() {
		// Given:
		MaterialAttachmentInputV1 attachment = null;

		// When:
		MaterialAttachmentInput actualResult = materialApiMapperImpl.toMaterialAttachmentInput(attachment);

		// Then:
		assertThat(actualResult).isNull();
	}

}