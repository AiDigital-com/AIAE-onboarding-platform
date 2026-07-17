package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UploadValidatorTest {

	private final UploadValidator validator = new UploadValidator();

	@Nested
	class ValidateTests {

		@Test
		void validate_nullFile_throwsAppExceptionC002() {
			// Execution
			assertThatThrownBy(() -> validator.validate(null))
					// Verification
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C002.name()));
		}

		@Test
		void validate_blankOriginalFilename_throwsAppExceptionC002() {
			// Given
			MockMultipartFile file = new MockMultipartFile("file", "", "application/pdf", new byte[100]);

			// Execution
			assertThatThrownBy(() -> validator.validate(file))
					// Verification
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C002.name()));
		}

		@Test
		void validate_zeroSize_throwsAppExceptionC002() {
			// Given
			MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[0]);

			// Execution
			assertThatThrownBy(() -> validator.validate(file))
					// Verification
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C002.name()));
		}

		@Test
		void validate_validFile_returnsRecordWithMatchingFields() {
			// Given
			MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[500]);

			// Execution
			UploadValidator.UploadValidationRecord result = validator.validate(file);

			// Verification
			assertThat(result.originalName()).isEqualTo("report.pdf");
			assertThat(result.mimeType()).isEqualTo("application/pdf");
			assertThat(result.sizeBytes()).isEqualTo(500L);
		}

		@Test
		void validate_nullContentType_returnsMimeTypeNullWithoutException() {
			// Given
			MockMultipartFile file = new MockMultipartFile("file", "data.bin", null, new byte[100]);

			// Execution
			UploadValidator.UploadValidationRecord result = validator.validate(file);

			// Verification
			assertThat(result.originalName()).isEqualTo("data.bin");
			assertThat(result.mimeType()).isNull();
			assertThat(result.sizeBytes()).isEqualTo(100L);
		}
	}
}
