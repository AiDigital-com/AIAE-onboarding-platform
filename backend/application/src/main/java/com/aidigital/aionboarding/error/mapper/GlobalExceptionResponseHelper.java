package com.aidigital.aionboarding.error.mapper;

import com.aidigital.aionboarding.api.v1.model.ApiErrorV1;
import com.aidigital.aionboarding.service.common.error.ValidationMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Builds the committed OpenAPI error response body for {@code GlobalExceptionHandler}, so the
 * handler stays limited to "which exception maps to which status" and carries no response-body
 * construction logic of its own (see {@code .claude/rules/30-web-openapi.md}: "Centralize error
 * mapping in GlobalExceptionHandler and its response helper").
 */
public interface GlobalExceptionResponseHelper {

	/**
	 * Builds the full response entity for a mapped error, including the timestamp and
	 * correlation id.
	 *
	 * @param message internal validation message describing the error
	 * @param status  HTTP status to return
	 * @return API error response entity
	 */
	ResponseEntity<ApiErrorV1> buildApiError(ValidationMessage message, HttpStatus status);
}
