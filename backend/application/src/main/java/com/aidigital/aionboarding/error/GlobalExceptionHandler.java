// GlobalExceptionHandler — single source of HTTP error translation.
// Catches AppException + Spring validation/security exceptions and converts
// them to the OpenAPI-generated ApiErrorV1 DTO. Controllers throw nothing
// of their own; services throw AppException with ErrorReason.

package com.aidigital.aionboarding.error;

import com.aidigital.aionboarding.api.v1.model.ApiErrorV1;
import com.aidigital.aionboarding.api.v1.model.ValidationParameterV1;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.error.ValidationMessage;
import com.aidigital.aionboarding.service.common.error.ValidationParameter;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.hibernate.LazyInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts service and framework exceptions into the committed OpenAPI error shape.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * MDC key for the per-request correlation id (set by a request filter).
	 */
	private static final String MDC_CORRELATION_ID = "correlationId";

	/**
	 * Error-code prefixes that map to specific HTTP statuses.
	 */
	private static final String NOT_FOUND_PREFIX = "C001";
	private static final String FORBIDDEN_PREFIX = "C004";
	private static final String UNAUTH_PREFIX = "C005";
	private static final String CONFLICT_PREFIX = "C006";
	private static final String RATE_LIMIT_PREFIX = "C007";
	private static final String VALIDATION_PREFIX = "V";

	/**
	 * Route-tag fallback for requests that never reached a mapped handler.
	 */
	private static final String UNMATCHED_ROUTE = "UNMATCHED";

	/**
	 * Counter name for {@link LazyInitializationException} occurrences, tagged by route.
	 */
	private static final String LAZY_INIT_METRIC = "app.errors.lazy_initialization";

	private final CurrentTime currentTime;
	private final MeterRegistry meterRegistry;

	/**
	 * Handles canonical application exceptions.
	 *
	 * @param ex application exception carrying an error code
	 * @return API error response with mapped HTTP status
	 */
	@ExceptionHandler(AppException.class)
	public ResponseEntity<ApiErrorV1> handleAppException(AppException ex) {
		HttpStatus status = statusForCode(ex.getCode());
		if (status.is5xxServerError()) {
			LOG.error("AppException {}: {}", ex.getCode(), ex.getMessage(), ex);
		} else {
			LOG.warn("AppException {}: {}", ex.getCode(), ex.getMessage());
		}
		return ResponseEntity.status(status).body(toDto(ex.getValidationMessage()));
	}

	/**
	 * Handles bean-validation failures.
	 *
	 * @param ex validation exception from Spring or Jakarta Validation
	 * @return 400 API error response
	 */
	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			ConstraintViolationException.class
	})
	public ResponseEntity<ApiErrorV1> handleValidation(Exception ex) {
		LOG.warn("Validation failed: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
				toDto(new ValidationMessage(ErrorReason.C002,
						new ValidationParameter("detail", ex.getMessage()))));
	}

	/**
	 * Handles authentication failures.
	 *
	 * @param ex Spring Security authentication exception
	 * @return 401 API error response
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorV1> handleAuth(AuthenticationException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
				toDto(new ValidationMessage(ErrorReason.C005)));
	}

	/**
	 * Handles authenticated callers without sufficient permissions.
	 *
	 * @param ex Spring Security authorization exception
	 * @return 403 API error response
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorV1> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
				toDto(new ValidationMessage(ErrorReason.C004,
						new ValidationParameter("detail", ex.getMessage()))));
	}

	/**
	 * Handles Hibernate lazy-association access outside an active session.
	 * <p>
	 * This failure mode is expensive to catch in production — it surfaces as a generic 500 with
	 * no functional symptom until a specific code path is exercised, and diagnosing it requires
	 * auditing every affected call site for a missing eager fetch (see the 2026-07
	 * {@code askLessonAssistant} incident). It is therefore counted separately from generic 500s
	 * via {@code app.errors.lazy_initialization}, tagged by route, so a recurrence is visible on
	 * a dashboard/alert instead of blending into background error noise.
	 *
	 * @param ex      the lazy-initialization failure
	 * @param request inbound request, used to tag the metric with the matched route template
	 * @return 500 API error response
	 */
	@ExceptionHandler(LazyInitializationException.class)
	public ResponseEntity<ApiErrorV1> handleLazyInitialization(LazyInitializationException ex,
															   HttpServletRequest request) {
		String route = resolveRouteTemplate(request);
		LOG.error("LazyInitializationException on {}: {}", route, ex.getMessage(), ex);
		meterRegistry.counter(LAZY_INIT_METRIC, "uri", route).increment();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				toDto(new ValidationMessage(ErrorReason.C000,
						new ValidationParameter("class", ex.getClass().getSimpleName()))));
	}

	/**
	 * Handles a lost concurrency race on a shared row: either an optimistic-locking conflict
	 * (the caller's copy of an entity was saved after someone else already changed the same row)
	 * or a pessimistic lock that could not be acquired before its configured timeout (someone
	 * else's transaction was still holding it). Applies uniformly to every {@code @Version}-ed
	 * entity and every pessimistic-lock query, so individual services do not each need their own
	 * conflict-mapping code.
	 *
	 * @param ex the concurrency failure — {@link ObjectOptimisticLockingFailureException} for a
	 *           version conflict, or a {@code PessimisticLockingFailureException} subtype for a
	 *           timed-out lock wait
	 * @return 409 API error response
	 */
	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<ApiErrorV1> handleConcurrencyFailure(ConcurrencyFailureException ex) {
		if (ex instanceof ObjectOptimisticLockingFailureException optimisticEx) {
			LOG.warn("Optimistic locking conflict on {}: {}", optimisticEx.getPersistentClassName(), ex.getMessage());
		} else {
			LOG.warn("Concurrency failure ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
		}
		return ResponseEntity.status(HttpStatus.CONFLICT).body(
				toDto(new ValidationMessage(ErrorReason.C006,
						new ValidationParameter("detail", "This item was changed by someone else. Reload and try again" +
								"."))));
	}

	/**
	 * Handles unexpected exceptions as opaque internal errors.
	 *
	 * @param ex unhandled exception
	 * @return 500 API error response
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorV1> handleUnknown(Exception ex) {
		LOG.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				toDto(new ValidationMessage(ErrorReason.C000,
						new ValidationParameter("class", ex.getClass().getSimpleName()))));
	}

	// ----- helpers -----

	/**
	 * Resolves the matched MVC route template for metric tagging, falling back to a fixed
	 * low-cardinality placeholder when no handler mapping matched (mirrors
	 * {@code PerformanceMetricsFilter}'s resolution so both keep the same route vocabulary).
	 *
	 * @param request inbound servlet request, after handler execution has started
	 * @return the matched route template, or {@code UNMATCHED}
	 */
	String resolveRouteTemplate(HttpServletRequest request) {
		Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		return pattern == null ? UNMATCHED_ROUTE : pattern.toString();
	}

	/**
	 * Maps the leading-prefix of an {@link ErrorReason} code to an HTTP status.
	 * Add a branch here when introducing a new cross-cutting code family
	 * (e.g. {@code C008} → {@code GONE}).
	 *
	 * @param code canonical error code
	 * @return HTTP status for the code family
	 */
	HttpStatus statusForCode(String code) {
		if (code == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		if (code.startsWith(NOT_FOUND_PREFIX)) {
			return HttpStatus.NOT_FOUND;
		}
		if (code.startsWith(FORBIDDEN_PREFIX)) {
			return HttpStatus.FORBIDDEN;
		}
		if (code.startsWith(UNAUTH_PREFIX)) {
			return HttpStatus.UNAUTHORIZED;
		}
		if (code.startsWith(CONFLICT_PREFIX)) {
			return HttpStatus.CONFLICT;
		}
		if (code.startsWith(RATE_LIMIT_PREFIX)) {
			return HttpStatus.TOO_MANY_REQUESTS;
		}
		if (code.startsWith(VALIDATION_PREFIX)) {
			return HttpStatus.BAD_REQUEST;
		}
		if (code.startsWith("C000")) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		// All other "Cxxx" codes and any domain-specific code → 400.
		return HttpStatus.BAD_REQUEST;
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
				.map(p -> {
					ValidationParameterV1 v = new ValidationParameterV1();
					v.setCode(p.getCode());
					v.setValue(p.getValue());
					return v;
				})
				.collect(Collectors.toList());
		dto.setParameters(params);
		return dto;
	}
}
