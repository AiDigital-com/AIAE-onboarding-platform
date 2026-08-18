package com.aidigital.aionboarding.service.material.services;

import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UploadValidatorTest {

	private final UploadValidator validator = new UploadValidator();

	@Nested
	class ValidateTests {

		@Test
		void validate_nullOriginalName_throwsAppExceptionC002() {
			// Execution
			assertThatThrownBy(() -> validator.validate(null, "application/pdf", 100L))
					// Verification
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C002.name()));
		}

		@Test
		void validate_blankOriginalFilename_throwsAppExceptionC002() {
			// Execution
			assertThatThrownBy(() -> validator.validate("", "application/pdf", 100L))
					// Verification
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C002.name()));
		}

		@Test
		void validate_zeroSize_throwsAppExceptionC002() {
			// Execution
			assertThatThrownBy(() -> validator.validate("doc.pdf", "application/pdf", 0L))
					// Verification
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C002.name()));
		}

		@Test
		void validate_negativeSize_throwsAppExceptionC002() {
			// Execution: this is the negative case for the split — before it, this method took a
			// MultipartFile and imported org.springframework.web.multipart, tripping
			// verify-gates.sh's "service source must not import web/security/JWT/servlet APIs"
			// assertion; now the method takes plain values with no web-framework type at all.
			assertThatThrownBy(() -> validator.validate("doc.pdf", "application/pdf", -1L))
					// Verification
					.isInstanceOf(AppException.class)
					.satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(ErrorReason.C002.name()));
		}

		@Test
		void validate_validInput_returnsRecordWithMatchingFields() {
			// Execution
			UploadValidator.UploadValidationRecord result = validator.validate("report.pdf", "application/pdf", 500L);

			// Verification
			assertThat(result.originalName()).isEqualTo("report.pdf");
			assertThat(result.mimeType()).isEqualTo("application/pdf");
			assertThat(result.sizeBytes()).isEqualTo(500L);
		}

		@Test
		void validate_nullContentType_returnsMimeTypeNullWithoutException() {
			// Execution
			UploadValidator.UploadValidationRecord result = validator.validate("data.bin", null, 100L);

			// Verification
			assertThat(result.originalName()).isEqualTo("data.bin");
			assertThat(result.mimeType()).isNull();
			assertThat(result.sizeBytes()).isEqualTo(100L);
		}
	}
}
