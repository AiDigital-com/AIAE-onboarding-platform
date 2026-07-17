package com.aidigital.aionboarding.mappers.lesson;

import com.aidigital.aionboarding.api.v1.model.LessonActivityTypeV1;
import com.aidigital.aionboarding.api.v1.model.LessonCreationModeV1;
import com.aidigital.aionboarding.api.v1.model.LessonGenerationStatusV1;
import com.aidigital.aionboarding.api.v1.model.LessonSortFieldV1;
import com.aidigital.aionboarding.api.v1.model.LessonVisibilityV1;
import com.aidigital.aionboarding.api.v1.model.SearchLessonsV1;
import com.aidigital.aionboarding.api.v1.model.SortDirectionV1;
import com.aidigital.aionboarding.service.lesson.models.LessonListQuery;
import com.aidigital.aionboarding.service.lesson.models.LessonSortField;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LessonApiMapperDefaultMethodsTest {

    private final LessonApiMapper mapper = new LessonApiMapperImpl();

    @Nested
    class ToLessonListQuery {

        @Test
        void shouldReturnNullFieldsWhenRequestIsNullTest() {
            // When:
            LessonListQuery result = mapper.toLessonListQuery(null);

            // Then:
            assertThat(result.searchText()).isNull();
            assertThat(result.tags()).isNull();
            assertThat(result.statusCode()).isNull();
            assertThat(result.publicationStatusCode()).isNull();
            assertThat(result.createdByUserId()).isNull();
            assertThat(result.assignedToMe()).isNull();
            assertThat(result.readyOnly()).isNull();
            assertThat(result.activityTypeCode()).isNull();
            assertThat(result.hasActivities()).isNull();
            assertThat(result.sortField()).isEqualTo(LessonSortField.CREATED_AT);
            assertThat(result.direction()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        void shouldMapAllFieldsWhenRequestIsFullyPopulatedTest() {
            // Given:
            SearchLessonsV1 request = new SearchLessonsV1();
            request.setQuery("q");
            request.setTags(List.of("t"));
            request.setStatus(LessonGenerationStatusV1.READY);
            request.setPublicationStatus(LessonVisibilityV1.PUBLISHED);
            request.setCreatedByUserId(5L);
            request.setAssignedToMe(true);
            request.setReadyOnly(false);
            request.setActivityType(LessonActivityTypeV1.QUIZ);
            request.setHasActivities(true);
            request.setSort(LessonSortFieldV1.TITLE);
            request.setDirection(SortDirectionV1.ASC);

            // When:
            LessonListQuery result = mapper.toLessonListQuery(request);

            // Then:
            assertThat(result.searchText()).isEqualTo("q");
            assertThat(result.tags()).containsExactly("t");
            assertThat(result.statusCode()).isEqualTo("ready");
            assertThat(result.publicationStatusCode()).isEqualTo("published");
            assertThat(result.createdByUserId()).isEqualTo(5L);
            assertThat(result.assignedToMe()).isTrue();
            assertThat(result.readyOnly()).isFalse();
            assertThat(result.activityTypeCode()).isEqualTo("quiz");
            assertThat(result.hasActivities()).isTrue();
            assertThat(result.sortField()).isEqualTo(LessonSortField.TITLE);
            assertThat(result.direction()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        void shouldUseNullForNullableFieldsWhenTheyAreNullTest() {
            // Given:
            SearchLessonsV1 request = new SearchLessonsV1();
            request.setStatus(null);
            request.setPublicationStatus(null);
            request.setActivityType(null);
            request.setDirection(null);

            // When:
            LessonListQuery result = mapper.toLessonListQuery(request);

            // Then:
            assertThat(result.statusCode()).isNull();
            assertThat(result.publicationStatusCode()).isNull();
            assertThat(result.activityTypeCode()).isNull();
            assertThat(result.direction()).isEqualTo(Sort.Direction.DESC);
        }
    }

    @Nested
    class PageAndSize {

        @Test
        void shouldReturnDefaultsForNullRequestTest() {
            // When-Then:
            assertThat(mapper.page(null)).isZero();
            assertThat(mapper.size(null)).isEqualTo(20);
        }

        @Test
        void shouldReturnDefaultsForNullPageAndSizeTest() {
            // Given:
            SearchLessonsV1 request = new SearchLessonsV1();

            // When-Then:
            assertThat(mapper.page(request)).isZero();
            assertThat(mapper.size(request)).isEqualTo(20);
        }

        @Test
        void shouldReturnProvidedPageAndSizeTest() {
            // Given:
            SearchLessonsV1 request = new SearchLessonsV1();
            request.setPage(3);
            request.setSize(50);

            // When-Then:
            assertThat(mapper.page(request)).isEqualTo(3);
            assertThat(mapper.size(request)).isEqualTo(50);
        }
    }

    @Nested
    class LessonSortFieldMapping {

        @Test
        void shouldDefaultToCreatedAtForNullRequestTest() {
            // When-Then:
            assertThat(mapper.lessonSortField(null)).isEqualTo(LessonSortField.CREATED_AT);
        }

        @Test
        void shouldDefaultToCreatedAtForNullSortTest() {
            // Given:
            SearchLessonsV1 request = new SearchLessonsV1();

            // When-Then:
            assertThat(mapper.lessonSortField(request)).isEqualTo(LessonSortField.CREATED_AT);
        }

        @Test
        void shouldMapEachSortFieldValueTest() {
            // When-Then:
            assertThat(mapper.lessonSortField(requestWithSort(LessonSortFieldV1.CREATED_AT)))
                .isEqualTo(LessonSortField.CREATED_AT);
            assertThat(mapper.lessonSortField(requestWithSort(LessonSortFieldV1.UPDATED_AT)))
                .isEqualTo(LessonSortField.UPDATED_AT);
            assertThat(mapper.lessonSortField(requestWithSort(LessonSortFieldV1.TITLE)))
                .isEqualTo(LessonSortField.TITLE);
        }

        private SearchLessonsV1 requestWithSort(LessonSortFieldV1 sort) {
            SearchLessonsV1 request = new SearchLessonsV1();
            request.setSort(sort);
            return request;
        }
    }

    @Nested
    class SortDirection {

        @Test
        void shouldDefaultToDescForNullRequestTest() {
            // When-Then:
            assertThat(mapper.sortDirection(null)).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        void shouldReturnAscForAscDirectionTest() {
            // Given:
            SearchLessonsV1 request = new SearchLessonsV1();
            request.setDirection(SortDirectionV1.ASC);

            // When-Then:
            assertThat(mapper.sortDirection(request)).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        void shouldReturnDescForDescDirectionTest() {
            // Given:
            SearchLessonsV1 request = new SearchLessonsV1();
            request.setDirection(SortDirectionV1.DESC);

            // When-Then:
            assertThat(mapper.sortDirection(request)).isEqualTo(Sort.Direction.DESC);
        }
    }

    @Nested
    class ParseDateTime {

        @Test
        void shouldReturnNullForNullOrBlankInputTest() {
            // When-Then:
            assertThat(mapper.parseDateTime(null)).isNull();
            assertThat(mapper.parseDateTime("   ")).isNull();
        }

        @Test
        void shouldParseLocalDateTimeTest() {
            // When:
            LocalDateTime result = mapper.parseDateTime("2026-07-15T10:00:00");

            // Then:
            assertThat(result).isEqualTo(LocalDateTime.parse("2026-07-15T10:00:00"));
        }

        @Test
        void shouldParseOffsetDateTimeTest() {
            // When:
            LocalDateTime result = mapper.parseDateTime("2026-07-15T10:00:00+02:00");

            // Then:
            assertThat(result).isEqualTo(LocalDateTime.parse("2026-07-15T10:00:00"));
        }

        @Test
        void shouldParseInstantTest() {
            // When:
            LocalDateTime result = mapper.parseDateTime("2026-07-15T10:00:00Z");

            // Then:
            assertThat(result).isEqualTo(LocalDateTime.parse("2026-07-15T10:00:00"));
        }

        @Test
        void shouldReturnNullForUnparseableInputTest() {
            // When-Then:
            assertThat(mapper.parseDateTime("not-a-date")).isNull();
        }
    }

    @Nested
    class ToBigDecimal {

        @Test
        void shouldReturnNullForNullInputTest() {
            // When-Then:
            assertThat(mapper.toBigDecimal(null)).isNull();
        }

        @Test
        void shouldReturnBigDecimalAsIsTest() {
            // Given:
            BigDecimal value = new BigDecimal("42.5");

            // When-Then:
            assertThat(mapper.toBigDecimal(value)).isSameAs(value);
        }

        @Test
        void shouldConvertNumberToBigDecimalTest() {
            // When-Then:
            assertThat(mapper.toBigDecimal(42L)).isEqualByComparingTo(BigDecimal.valueOf(42));
            assertThat(mapper.toBigDecimal(42.5)).isEqualByComparingTo(BigDecimal.valueOf(42.5));
        }

        @Test
        void shouldParseStringToBigDecimalTest() {
            // When-Then:
            assertThat(mapper.toBigDecimal("42.5")).isEqualByComparingTo(new BigDecimal("42.5"));
        }

        @Test
        void shouldReturnNullForInvalidStringTest() {
            // When-Then:
            assertThat(mapper.toBigDecimal("invalid")).isNull();
        }
    }

    @Nested
    class ToLessonCreationMode {

        @Test
        void shouldReturnNullForNullActionTest() {
            // When-Then:
            assertThat(mapper.toLessonCreationMode(null)).isNull();
        }

        @Test
        void shouldMapGenerateActionTest() {
            // When-Then:
            assertThat(mapper.toLessonCreationMode(LessonCreationModeV1.GENERATE))
                .isEqualTo(com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1.GENERATE);
        }

        @Test
        void shouldMapCreateManualActionTest() {
            // When-Then:
            assertThat(mapper.toLessonCreationMode(LessonCreationModeV1.CREATE_MANUAL))
                .isEqualTo(com.aidigital.aionboarding.service.lesson.enums.LessonCreationModeV1.CREATE_MANUAL);
        }
    }
}
