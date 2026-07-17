package com.aidigital.aionboarding.mappers.roadmap;

import com.aidigital.aionboarding.api.v1.model.CreateRoadmapRequestV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapLessonV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapResponseV1;
import com.aidigital.aionboarding.api.v1.model.RoadmapV1;
import com.aidigital.aionboarding.api.v1.model.UpdateRoadmapRequestV1;
import com.aidigital.aionboarding.mappers.common.LessonGenerationStatusApiMapper;
import com.aidigital.aionboarding.service.roadmap.models.CreateRoadmapInput;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapLessonRecord;
import com.aidigital.aionboarding.service.roadmap.models.RoadmapRecord;
import com.aidigital.aionboarding.service.roadmap.models.UpdateRoadmapInput;
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
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoadmapApiMapperImplTest {

	@InjectMocks
	private RoadmapApiMapperImpl roadmapApiMapperImpl;

	@Mock
	private LessonGenerationStatusApiMapper lessonGenerationStatusApiMapper;

	@BeforeEach
	void setUp() {
		when(lessonGenerationStatusApiMapper.mapLessonGenerationStatus(anyString())).thenReturn(Instancio.create(LessonGenerationStatusV1.class));
	}

	@Test
	void shouldToRoadmapV1RoadmapRecordTest() {
		// Given:
		RoadmapRecord roadmap = Instancio.create(RoadmapRecord.class);

		// When:
		RoadmapV1 actualResult = roadmapApiMapperImpl.toRoadmapV1(roadmap);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapV1RoadmapRecordWithNullTest() {
		// Given:
		RoadmapRecord roadmap = null;

		// When:
		RoadmapV1 actualResult = roadmapApiMapperImpl.toRoadmapV1(roadmap);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapLessonV1RoadmapLessonRecordTest() {
		// Given:
		RoadmapLessonRecord lesson = Instancio.create(RoadmapLessonRecord.class);

		// When:
		RoadmapLessonV1 actualResult = roadmapApiMapperImpl.toRoadmapLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapLessonV1RoadmapLessonRecordWithNullTest() {
		// Given:
		RoadmapLessonRecord lesson = null;

		// When:
		RoadmapLessonV1 actualResult = roadmapApiMapperImpl.toRoadmapLessonV1(lesson);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToRoadmapResponseV1RoadmapRecordTest() {
		// Given:
		RoadmapRecord roadmap = Instancio.create(RoadmapRecord.class);

		// When:
		RoadmapResponseV1 actualResult = roadmapApiMapperImpl.toRoadmapResponseV1(roadmap);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToRoadmapResponseV1RoadmapRecordWithNullTest() {
		// Given:
		RoadmapRecord roadmap = null;

		// When:
		RoadmapResponseV1 actualResult = roadmapApiMapperImpl.toRoadmapResponseV1(roadmap);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToCreateRoadmapInputCreateRoadmapRequestV1Test() {
		// Given:
		CreateRoadmapRequestV1 request = Instancio.create(CreateRoadmapRequestV1.class);

		// When:
		CreateRoadmapInput actualResult = roadmapApiMapperImpl.toCreateRoadmapInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToCreateRoadmapInputCreateRoadmapRequestV1WithNullTest() {
		// Given:
		CreateRoadmapRequestV1 request = null;

		// When:
		CreateRoadmapInput actualResult = roadmapApiMapperImpl.toCreateRoadmapInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}


	@Test
	void shouldToUpdateRoadmapInputUpdateRoadmapRequestV1Test() {
		// Given:
		UpdateRoadmapRequestV1 request = Instancio.create(UpdateRoadmapRequestV1.class);

		// When:
		UpdateRoadmapInput actualResult = roadmapApiMapperImpl.toUpdateRoadmapInput(request);

		// Then:
		assertThat(actualResult).isNotNull();
	}


	@Test
	void shouldToUpdateRoadmapInputUpdateRoadmapRequestV1WithNullTest() {
		// Given:
		UpdateRoadmapRequestV1 request = null;

		// When:
		UpdateRoadmapInput actualResult = roadmapApiMapperImpl.toUpdateRoadmapInput(request);

		// Then:
		assertThat(actualResult).isNull();
	}

}