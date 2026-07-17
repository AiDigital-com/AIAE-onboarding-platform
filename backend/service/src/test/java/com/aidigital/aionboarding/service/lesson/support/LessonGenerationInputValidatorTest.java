package com.aidigital.aionboarding.service.lesson.support;

import com.aidigital.aionboarding.domain.material.entities.Material;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;

import org.instancio.Instancio;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;

class LessonGenerationInputValidatorTest {

	private final LessonGenerationInputValidator validator = new LessonGenerationInputValidator();

	@Nested
	class DeduplicatePreserveOrder {

		@Test
		void shouldRemoveDuplicatesWhilePreservingFirstOccurrenceOrderTest() {
			// Given:
			List<Long> materialIds = List.of(3L, 1L, 3L, 2L, 1L);

			// When:
			List<Long> result = validator.deduplicatePreserveOrder(materialIds);

			// Then:
			assertThat(result).containsExactly(3L, 1L, 2L);
		}

		@Test
		void shouldReturnEmptyListWhenInputIsEmptyTest() {
			// Given:
			List<Long> materialIds = List.of();

			// When:
			List<Long> result = validator.deduplicatePreserveOrder(materialIds);

			// Then:
			assertThat(result).isEmpty();
		}

		@Test
		void shouldReturnSameOrderWhenNoDuplicatesArePresentTest() {
			// Given:
			List<Long> materialIds = List.of(5L, 4L, 6L);

			// When:
			List<Long> result = validator.deduplicatePreserveOrder(materialIds);

			// Then:
			assertThat(result).containsExactly(5L, 4L, 6L);
		}
	}

	@Nested
	class ValidateManualLesson {

		@Test
		void shouldThrowV001WhenTitleIsNullTest() {
			// When-Then:
			assertThatThrownBy(() -> validator.validateManualLesson(null, "Some content"))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Manual lesson requires a title and non-empty content.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldThrowV001WhenTitleIsBlankTest() {
			// When-Then:
			assertThatThrownBy(() -> validator.validateManualLesson("   ", "Some content"))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Manual lesson requires a title and non-empty content.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldThrowV001WhenContentHtmlIsNullTest() {
			// When-Then:
			assertThatThrownBy(() -> validator.validateManualLesson("Title", null))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Manual lesson requires a title and non-empty content.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldThrowV001WhenContentHtmlIsBlankTest() {
			// When-Then:
			assertThatThrownBy(() -> validator.validateManualLesson("Title", "   "))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Manual lesson requires a title and non-empty content.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldNotThrowWhenTitleAndContentAreBothNonBlankTest() {
			// When-Then:
			assertThatCode(() -> validator.validateManualLesson("Title", "<p>Content</p>"))
					.doesNotThrowAnyException();
		}
	}

	@Nested
	class ValidateMaterialsUsable {

		@Test
		void shouldThrowV001WhenNoMaterialsRequestedAndInstructionsAreNullTest() {
			// Given:
			List<Long> requestedIds = List.of();
			List<Material> materials = List.of();

			// When-Then:
			assertThatThrownBy(() -> validator.validateMaterialsUsable(requestedIds, materials, null))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Select at least one material or describe what the lesson should be about.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldThrowV001WhenNoMaterialsRequestedAndInstructionsAreBlankTest() {
			// Given:
			List<Long> requestedIds = List.of();
			List<Material> materials = List.of();

			// When-Then:
			assertThatThrownBy(() -> validator.validateMaterialsUsable(requestedIds, materials, "   "))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Select at least one material or describe what the lesson should be about.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldReturnWithoutThrowingWhenNoMaterialsRequestedButInstructionsAreProvidedTest() {
			// Given:
			List<Long> requestedIds = List.of();
			List<Material> materials = List.of();

			// When-Then:
			assertThatCode(() -> validator.validateMaterialsUsable(requestedIds, materials, "Explain onboarding " +
					"basics"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowV001WhenMaterialsWereRequestedButNoneResolvedAndNoInstructionsTest() {
			// Given: ids were requested but resolution returned no materials (e.g. deleted or inaccessible)
			List<Long> requestedIds = List.of(1L, 2L);
			List<Material> materials = List.of();

			// When-Then:
			assertThatThrownBy(() -> validator.validateMaterialsUsable(requestedIds, materials, ""))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Select at least one material or describe what the lesson should be about.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldReturnWithoutThrowingWhenMaterialsWereRequestedButNoneResolvedAndInstructionsAreProvidedTest() {
			// Given:
			List<Long> requestedIds = List.of(1L, 2L);
			List<Material> materials = List.of();

			// When-Then:
			assertThatCode(() -> validator.validateMaterialsUsable(requestedIds, materials, "Focus on password " +
					"resets"))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldReturnWithoutThrowingWhenAllResolvedMaterialsAreUsableTest() {
			// Given:
			Material usableMaterial = Instancio.of(Material.class)
					.set(field(Material::getId), 10L)
					.set(field(Material::getTitle), "Company handbook")
					.create();
			List<Long> requestedIds = List.of(10L);

			// When-Then:
			assertThatCode(() -> validator.validateMaterialsUsable(requestedIds, List.of(usableMaterial), null))
					.doesNotThrowAnyException();
		}

		@Test
		void shouldThrowV001NamingTheFirstUnusableMaterialTest() {
			// Given: first material is usable, second has no usable content in any field
			Material usableMaterial = Instancio.of(Material.class)
					.set(field(Material::getId), 10L)
					.set(field(Material::getTitle), "Company handbook")
					.create();
			Material unusableMaterial = Instancio.of(Material.class)
					.set(field(Material::getId), 20L)
					.set(field(Material::getTitle), "")
					.set(field(Material::getDescription), "")
					.set(field(Material::getTextContent), "")
					.create();
			List<Long> requestedIds = List.of(10L, 20L);

			// When-Then:
			assertThatThrownBy(() -> validator.validateMaterialsUsable(
					requestedIds, List.of(usableMaterial, unusableMaterial), null))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Material 20 has no usable content.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}

		@Test
		void shouldThrowV001ForAnUnusableMaterialEvenWhenInstructionsAreProvidedTest() {
			// Given: instructions do not bypass the per-material usability check
			Material unusableMaterial = Instancio.of(Material.class)
					.set(field(Material::getId), 30L)
					.set(field(Material::getTitle), "   ")
					.set(field(Material::getDescription), "")
					.set(field(Material::getTextContent), null)
					.create();
			List<Long> requestedIds = List.of(30L);

			// When-Then:
			assertThatThrownBy(() -> validator.validateMaterialsUsable(
					requestedIds, List.of(unusableMaterial), "Explain onboarding basics"))
					.isInstanceOf(AppException.class)
					.hasMessageContaining("Material 30 has no usable content.")
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.V001.name()));
		}
	}

	@Nested
	class IsMaterialUsable {

		@Test
		void shouldReturnFalseWhenMaterialIsNullTest() {
			// When-Then:
			assertThat(validator.isMaterialUsable(null)).isFalse();
		}

		@Test
		void shouldReturnFalseWhenTitleDescriptionAndTextContentAreAllBlankTest() {
			// Given:
			Material material = Instancio.of(Material.class)
					.set(field(Material::getTitle), "")
					.set(field(Material::getDescription), "   ")
					.set(field(Material::getTextContent), null)
					.create();

			// When-Then:
			assertThat(validator.isMaterialUsable(material)).isFalse();
		}

		@Test
		void shouldReturnTrueWhenOnlyTitleIsNonBlankTest() {
			// Given:
			Material material = Instancio.of(Material.class)
					.set(field(Material::getTitle), "Company handbook")
					.set(field(Material::getDescription), "")
					.set(field(Material::getTextContent), "")
					.create();

			// When-Then:
			assertThat(validator.isMaterialUsable(material)).isTrue();
		}

		@Test
		void shouldReturnTrueWhenOnlyDescriptionIsNonBlankTest() {
			// Given:
			Material material = Instancio.of(Material.class)
					.set(field(Material::getTitle), "")
					.set(field(Material::getDescription), "Policies and procedures")
					.set(field(Material::getTextContent), "")
					.create();

			// When-Then:
			assertThat(validator.isMaterialUsable(material)).isTrue();
		}

		@Test
		void shouldReturnTrueWhenOnlyTextContentIsNonBlankTest() {
			// Given:
			Material material = Instancio.of(Material.class)
					.set(field(Material::getTitle), "")
					.set(field(Material::getDescription), "")
					.set(field(Material::getTextContent), "Extracted body text")
					.create();

			// When-Then:
			assertThat(validator.isMaterialUsable(material)).isTrue();
		}
	}

	@Nested
	class BuildDraftTitle {

		@Test
		void shouldReturnPlaceholderWhenMaterialsListIsEmptyTest() {
			// When-Then:
			assertThat(validator.buildDraftTitle(List.of())).isEqualTo("Generating theoretical lesson");
		}

		@Test
		void shouldReturnPlaceholderWhenAllMaterialTitlesAreBlankTest() {
			// Given:
			Material blankTitle = Instancio.of(Material.class).set(field(Material::getTitle), "   ").create();

			// When-Then:
			assertThat(validator.buildDraftTitle(List.of(blankTitle))).isEqualTo("Generating theoretical lesson");
		}

		@Test
		void shouldReturnSingleTitleWhenOnlyOneMaterialHasAUsableTitleTest() {
			// Given: a blank-titled material is filtered out before the remaining titles are counted
			Material blankTitle = Instancio.of(Material.class).set(field(Material::getTitle), "").create();
			Material namedMaterial = Instancio.of(Material.class)
					.set(field(Material::getTitle), "Company handbook")
					.create();

			// When-Then:
			assertThat(validator.buildDraftTitle(List.of(blankTitle, namedMaterial))).isEqualTo("Company handbook");
		}

		@Test
		void shouldJoinTwoTitlesWithAPlusSignTest() {
			// Given:
			Material first = Instancio.of(Material.class).set(field(Material::getTitle), "Company handbook").create();
			Material second =
					Instancio.of(Material.class).set(field(Material::getTitle), "Onboarding checklist").create();

			// When-Then:
			assertThat(validator.buildDraftTitle(List.of(first, second)))
					.isEqualTo("Company handbook + Onboarding checklist");
		}

		@Test
		void shouldUseFirstTitleAndRemainingCountWhenThreeOrMoreTitlesArePresentTest() {
			// Given:
			Material first = Instancio.of(Material.class).set(field(Material::getTitle), "Company handbook").create();
			Material second =
					Instancio.of(Material.class).set(field(Material::getTitle), "Onboarding checklist").create();
			Material third = Instancio.of(Material.class).set(field(Material::getTitle), "Security policy").create();

			// When-Then:
			assertThat(validator.buildDraftTitle(List.of(first, second, third)))
					.isEqualTo("Company handbook and 2 more materials");
		}
	}

	@Nested
	class IsNonBlank {

		@Test
		void shouldReturnFalseWhenValueIsNullTest() {
			// When-Then:
			assertThat(validator.isNonBlank(null)).isFalse();
		}

		@Test
		void shouldReturnFalseWhenValueIsEmptyTest() {
			// When-Then:
			assertThat(validator.isNonBlank("")).isFalse();
		}

		@Test
		void shouldReturnFalseWhenValueIsOnlyWhitespaceTest() {
			// When-Then:
			assertThat(validator.isNonBlank("   ")).isFalse();
		}

		@Test
		void shouldReturnTrueWhenValueHasNonWhitespaceContentTest() {
			// When-Then:
			assertThat(validator.isNonBlank("hello")).isTrue();
		}
	}
}
