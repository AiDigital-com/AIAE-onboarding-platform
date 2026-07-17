package com.aidigital.aionboarding.service.common.mapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextValueNormalizerTest {

	private final TextValueNormalizer normalizer = new TextValueNormalizer();

	@Test
	void trimmedShouldReturnEmptyForNullTest() {
		assertThat(normalizer.trimmed(null)).isEmpty();
	}

	@Test
	void trimmedShouldTrimStringValuesTest() {
		assertThat(normalizer.trimmed("  value  ")).isEqualTo("value");
	}

	@Test
	void rawShouldPreserveStringWhitespaceTest() {
		assertThat(normalizer.raw("  value  ")).isEqualTo("  value  ");
	}

	@Test
	void shouldConvertNumericValuesTest() {
		assertThat(normalizer.trimmed(123)).isEqualTo("123");
		assertThat(normalizer.raw(123)).isEqualTo("123");
	}

	@Test
	void firstNonBlankTrimmedShouldReturnTrimmedCandidateTest() {
		assertThat(normalizer.firstNonBlankTrimmed(" ", "  a  ")).isEqualTo("a");
	}

	@Test
	void firstNonBlankRawShouldReturnRawCandidateTest() {
		assertThat(normalizer.firstNonBlankRaw(" ", "  a  ")).isEqualTo("  a  ");
	}

	@Test
	void firstNonBlankShouldReturnEmptyWhenNoCandidateTest() {
		assertThat(normalizer.firstNonBlankTrimmed(null, " ")).isEmpty();
		assertThat(normalizer.firstNonBlankRaw(null, " ")).isEmpty();
	}
}
