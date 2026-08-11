package com.aidigital.aionboarding.error.mapper;

import com.aidigital.aionboarding.api.v1.model.ApiErrorV1;
import com.aidigital.aionboarding.api.v1.model.ValidationParameterV1;
import com.aidigital.aionboarding.service.common.error.ValidationMessage;
import com.aidigital.aionboarding.service.common.error.ValidationParameter;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default {@link GlobalExceptionResponseHelper} implementation. Owns every piece of the wire
 * payload that is not "which status applies" — the timestamp, the correlation id lookup, and
 * the internal-to-generated-DTO field mapping — so {@code GlobalExceptionHandler} itself needs
 * neither {@link CurrentTime} nor the MDC correlation-id key.
 */
@Component
@RequiredArgsConstructor
public class GlobalExceptionResponseHelperImpl implements GlobalExceptionResponseHelper {

	/**
	 * MDC key for the per-request correlation id (set by a request filter).
	 */
	private static final String MDC_CORRELATION_ID = "correlationId";

	private final CurrentTime currentTime;

	/**
	 * Builds the full response entity for a mapped error, including the timestamp and
	 * correlation id.
	 *
	 * @param message internal validation message describing the error
	 * @param status  HTTP status to return
	 * @return API error response entity
	 */
	@Override
	public ResponseEntity<ApiErrorV1> buildApiError(ValidationMessage message, HttpStatus status) {
		return ResponseEntity.status(status).body(toDto(message));
	}

	/**
	 * Builds the ApiErrorV1 wire payload from an internal {@link ValidationMessage}.
	 * Timestamp is recorded in UTC as a {@code LocalDateTime} to match the
	 * project-wide time convention (see backend SKILL "Time types").
	 *
	 * @param msg internal validation message
	 * @return OpenAPI error response DTO
	 */
	ApiErrorV1 toDto(ValidationMessage msg) {
		ApiErrorV1 dto = new ApiErrorV1();
		dto.setCode(msg.getCode());
		dto.setMessage(msg.getMessage());
		dto.setTimestamp(currentTime.utcDateTime());
		dto.setCorrelationId(MDC.get(MDC_CORRELATION_ID));
		List<ValidationParameterV1> params = msg.getParameters().stream()
				.map(this::toParameterDto)
				.collect(Collectors.toList());
		dto.setParameters(params);
		return dto;
	}

	/**
	 * Maps an internal {@link ValidationParameter} to the generated OpenAPI parameter DTO.
	 *
	 * @param parameter internal validation parameter
	 * @return generated parameter DTO
	 */
	ValidationParameterV1 toParameterDto(ValidationParameter parameter) {
		ValidationParameterV1 v = new ValidationParameterV1();
		v.setCode(parameter.getCode());
		v.setValue(parameter.getValue());
		return v;
	}
}
