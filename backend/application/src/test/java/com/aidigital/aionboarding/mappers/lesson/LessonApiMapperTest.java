package com.aidigital.aionboarding.mappers.lesson;

import com.aidigital.aionboarding.api.v1.model.ChatMessageV1;
import com.aidigital.aionboarding.api.v1.model.LessonSortFieldV1;
import com.aidigital.aionboarding.api.v1.model.SearchLessonsV1;
import com.aidigital.aionboarding.api.v1.model.SortDirectionV1;
import com.aidigital.aionboarding.service.learning.services.LearningEnrollmentService;
import com.aidigital.aionboarding.service.lesson.models.ChatTurn;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSearchSummaryRecord;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

/**
 * Exercises {@link LessonApiMapper} default methods directly against the generated
 * implementation, since MapStruct default methods are otherwise mocked out (returning
 * canned values) everywhere else in the test suite.
 */
class LessonApiMapperTest {

	private final LessonApiMapper mapper = new LessonApiMapperImpl();

	@Nested
	class ToChatHistory {

		@Test
		void shouldReturnEmptyForNullTest() {
			// When:
			List<ChatTurn> result = mapper.toChatHistory(null);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldMapMessagesTest() {
			// Given:
			ChatMessageV1 msg = Instancio.of(ChatMessageV1.class)
					.set(field("content"), "Hello")
					.create();

			// When:
			List<ChatTurn> result = mapper.toChatHistory(List.of(msg));

			// Then:
			assertThat(result).hasSize(1);
			assertThat(result.get(0).content()).isEqualTo("Hello");
		}

		@Test
		void shouldSkipNullMessagesTest() {
			// When:
			List<ChatTurn> result = mapper.toChatHistory(Arrays.asList(null, null));

			// Then:
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class PageAndSize {

		@Test
		void shouldDefaultPageAndSizeWhenRequestIsNullTest() {
			// When / Then:
			assertThat(mapper.page(null)).isZero();
			assertThat(mapper.size(null)).isEqualTo(20);
		}

		@Test
		void shouldUseRequestedPageAndSizeWhenPresentTest() {
			// Given:
			SearchLessonsV1 request = new SearchLessonsV1().page(3).size(50);

			// When / Then:
			assertThat(mapper.page(request)).isEqualTo(3);
			assertThat(mapper.size(request)).isEqualTo(50);
		}
	}

	@Nested
	class ToLessonListQuery {

		@Test
		void shouldReturnAllNullFieldsForANullRequestTest() {
			// When:
			LessonListQuery query = mapper.toLessonListQuery(null);

			// Then:
			assertThat(query.searchText()).isNull();
			assertThat(query.sortField()).isEqualTo(LessonSortField.CREATED_AT);
			assertThat(query.direction()).isEqualTo(Sort.Direction.DESC);
		}

		@Test
		void shouldCarryEveryFieldFromAPopulatedRequestTest() {
			// Given:
			SearchLessonsV1 request = new SearchLessonsV1()
					.query("intro")
					.createdByUserId(5L)
					.assignedToMe(true)
					.readyOnly(true)
					.hasActivities(true)
					.learnableOnly(true)
					.sort(LessonSortFieldV1.UPDATED_AT)
					.direction(SortDirectionV1.ASC);

			// When:
			LessonListQuery query = mapper.toLessonListQuery(request);

			// Then:
			assertThat(query.searchText()).isEqualTo("intro");
			assertThat(query.createdByUserId()).isEqualTo(5L);
			assertThat(query.assignedToMe()).isTrue();
			assertThat(query.readyOnly()).isTrue();
			assertThat(query.hasActivities()).isTrue();
			assertThat(query.learnableOnly()).isTrue();
			assertThat(query.sortField()).isEqualTo(LessonSortField.UPDATED_AT);
			assertThat(query.direction()).isEqualTo(Sort.Direction.ASC);
		}

		@Test
		void shouldReturnNullLearnableOnlyWhenRequestOmitsItTest() {
			// Given:
			SearchLessonsV1 request = new SearchLessonsV1().query("intro");

			// When:
			LessonListQuery query = mapper.toLessonListQuery(request);

			// Then:
			assertThat(query.learnableOnly()).isNull();
		}
	}

	@Nested
	class LessonSortFieldAndDirection {

		@Test
		void shouldMapEverySortFieldTest() {
			// When / Then:
			assertThat(mapper.lessonSortField(new SearchLessonsV1().sort(LessonSortFieldV1.CREATED_AT)))
					.isEqualTo(LessonSortField.CREATED_AT);
			assertThat(mapper.lessonSortField(new SearchLessonsV1().sort(LessonSortFieldV1.UPDATED_AT)))
					.isEqualTo(LessonSortField.UPDATED_AT);
			assertThat(mapper.lessonSortField(new SearchLessonsV1().sort(LessonSortFieldV1.TITLE)))
					.isEqualTo(LessonSortField.TITLE);
			assertThat(mapper.lessonSortField(null)).isEqualTo(LessonSortField.CREATED_AT);
			assertThat(mapper.lessonSortField(new SearchLessonsV1())).isEqualTo(LessonSortField.CREATED_AT);
		}

		@Test
		void shouldMapSortDirectionTest() {
			// When / Then:
			assertThat(mapper.sortDirection(new SearchLessonsV1().direction(SortDirectionV1.ASC)))
					.isEqualTo(Sort.Direction.ASC);
			assertThat(mapper.sortDirection(new SearchLessonsV1().direction(SortDirectionV1.DESC)))
					.isEqualTo(Sort.Direction.DESC);
			assertThat(mapper.sortDirection(null)).isEqualTo(Sort.Direction.DESC);
		}
	}

	@Nested
	class ParseDateTimeAndBigDecimal {

		@Test
		void shouldReturnNullForBlankOrNullInputTest() {
			// When / Then:
			assertThat(mapper.parseDateTime(null)).isNull();
			assertThat(mapper.parseDateTime("  ")).isNull();
			assertThat(mapper.parseDateTime("not-a-date")).isNull();
		}

		@Test
		void shouldParseLocalOffsetAndInstantFormsTest() {
			// When / Then:
			assertThat(mapper.parseDateTime("2026-01-01T00:00:00")).isNotNull();
			assertThat(mapper.parseDateTime("2026-01-01T00:00:00+02:00")).isNotNull();
			assertThat(mapper.parseDateTime("2026-01-01T00:00:00Z")).isNotNull();
		}

		@Test
		void shouldConvertVariousNumberShapesToBigDecimalTest() {
			// When / Then:
			assertThat(mapper.toBigDecimal(null)).isNull();
			assertThat(mapper.toBigDecimal(BigDecimal.TEN)).isEqualTo(BigDecimal.TEN);
			assertThat(mapper.toBigDecimal(5)).isEqualTo(BigDecimal.valueOf(5.0));
			assertThat(mapper.toBigDecimal("not-a-number")).isNull();
			assertThat(mapper.toBigDecimal("3.5")).isEqualTo(new BigDecimal("3.5"));
		}
	}

	@Nested
	class ToLessonsListResponseV1 {

		@Test
		void shouldReturnNullForANullPageTest() {
			// When:
			var result = mapper.toLessonsListResponseV1(null);

			// Then:
			assertThat(result).isNull();
		}

		@Test
		void shouldBuildAnEmptyResponseForAnEmptyPageTest() {
			// Given:
			Page<LessonSearchSummaryRecord> page = new PageImpl<>(List.of());

			// When:
			var result = mapper.toLessonsListResponseV1(page);

			// Then:
			assertThat(result.getLessons()).isEmpty();
			assertThat(result.getPage().getTotalElements()).isZero();
		}

		@Test
		void shouldNotCallTheEnrollmentServiceForAnEmptyPageTest() {
			// Given:
			Page<LessonSearchSummaryRecord> page = new PageImpl<>(List.of());
			LearningEnrollmentService learningEnrollmentService = Mockito.mock(LearningEnrollmentService.class);

			// When:
			var result = mapper.toLessonsListResponseV1(page, 1L, learningEnrollmentService);

			// Then:
			assertThat(result.getLessons()).isEmpty();
			Mockito.verifyNoInteractions(learningEnrollmentService);
		}
	}
}
