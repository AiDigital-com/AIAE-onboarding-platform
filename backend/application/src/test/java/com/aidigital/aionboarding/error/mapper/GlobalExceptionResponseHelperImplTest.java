package com.aidigital.aionboarding.error.mapper;

import com.aidigital.aionboarding.api.v1.model.ApiErrorV1;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.error.ValidationMessage;
import com.aidigital.aionboarding.service.common.error.ValidationParameter;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionResponseHelperImplTest {

	@Mock
	private CurrentTime currentTime;

	@Test
	void shouldBuildResponseEntityWithMappedStatusAndTimestampTest() {
		// Given:
		LocalDateTime now = LocalDateTime.of(2026, 8, 11, 12, 0);
		when(currentTime.utcDateTime()).thenReturn(now);
		GlobalExceptionResponseHelperImpl helper = new GlobalExceptionResponseHelperImpl(currentTime);
		ValidationMessage message = new ValidationMessage(ErrorReason.C001, 42L);

		// When:
		ResponseEntity<ApiErrorV1> resp = helper.buildApiError(message, HttpStatus.NOT_FOUND);

		// Then:
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		ApiErrorV1 body = resp.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getCode()).isEqualTo("C001");
		assertThat(body.getTimestamp()).isEqualTo(now);
		assertThat(body.getParameters()).isNotEmpty();
	}

	@Test
	void shouldMapEveryValidationParameterToItsOwnDtoTest() {
		// Given: this is the negative case for the split — before the split, parameter mapping
		// lived inline inside GlobalExceptionHandler.toDto and was untestable without also
		// exercising a full exception handler; now it is independently verifiable here.
		when(currentTime.utcDateTime()).thenReturn(LocalDateTime.now(ZoneOffset.UTC));
		GlobalExceptionResponseHelperImpl helper = new GlobalExceptionResponseHelperImpl(currentTime);
		ValidationMessage message = new ValidationMessage(ErrorReason.C004,
				new ValidationParameter("detail", "no permission"));

		// When:
		ResponseEntity<ApiErrorV1> resp = helper.buildApiError(message, HttpStatus.FORBIDDEN);

		// Then:
		assertThat(resp.getBody().getParameters()).hasSize(1);
		assertThat(resp.getBody().getParameters().get(0).getCode()).isEqualTo("detail");
		assertThat(resp.getBody().getParameters().get(0).getValue()).isEqualTo("no permission");
	}

	@Test
	void shouldFallBackToNullCorrelationIdWhenMdcHasNoneTest() {
		// Given:
		MDC.clear();
		when(currentTime.utcDateTime()).thenReturn(LocalDateTime.now(ZoneOffset.UTC));
		GlobalExceptionResponseHelperImpl helper = new GlobalExceptionResponseHelperImpl(currentTime);

		// When:
		ResponseEntity<ApiErrorV1> resp = helper.buildApiError(new ValidationMessage(ErrorReason.C000, "x"),
				HttpStatus.INTERNAL_SERVER_ERROR);

		// Then:
		assertThat(resp.getBody().getCorrelationId()).isNull();
	}

	@Test
	void shouldUseCorrelationIdFromMdcWhenPresentTest() {
		// Given:
		MDC.put("correlationId", "req-123");
		when(currentTime.utcDateTime()).thenReturn(LocalDateTime.now(ZoneOffset.UTC));
		GlobalExceptionResponseHelperImpl helper = new GlobalExceptionResponseHelperImpl(currentTime);

		try {
			// When:
			ResponseEntity<ApiErrorV1> resp = helper.buildApiError(new ValidationMessage(ErrorReason.C000, "x"),
					HttpStatus.INTERNAL_SERVER_ERROR);

			// Then:
			assertThat(resp.getBody().getCorrelationId()).isEqualTo("req-123");
		} finally {
			MDC.clear();
		}
	}
}
