package com.aidigital.aionboarding.error;

import com.aidigital.aionboarding.api.v1.model.ApiErrorV1;
import com.aidigital.aionboarding.service.common.error.AppException;
import com.aidigital.aionboarding.service.common.error.ErrorReason;
import com.aidigital.aionboarding.service.common.time.CurrentTime;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new CurrentTime(),
			new SimpleMeterRegistry());

	@Test
	void shouldMapAppExceptionCodesToHttpStatusesTest() {
		// Given / When / Then: each ErrorReason prefix maps to its HTTP status
		assertThat(status(ErrorReason.C001)).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(status(ErrorReason.C004)).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(status(ErrorReason.C005)).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(status(ErrorReason.C006)).isEqualTo(HttpStatus.CONFLICT);
		assertThat(status(ErrorReason.C007)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(status(ErrorReason.C000)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(status(ErrorReason.C002)).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	private HttpStatus status(ErrorReason reason) {
		return (HttpStatus) handler.handleAppException(new AppException(reason, "x")).getStatusCode();
	}

	@Test
	void shouldPopulateApiErrorBodyTest() {
		// When:
		ResponseEntity<ApiErrorV1> resp = handler.handleAppException(new AppException(ErrorReason.C001, 42L));

		// Then:
		ApiErrorV1 body = resp.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getCode()).isEqualTo("C001");
		assertThat(body.getMessage()).contains("Resource not found");
		assertThat(body.getTimestamp()).isNotNull();
		assertThat(body.getParameters()).isNotEmpty();
	}

	@Test
	void shouldMapValidationToBadRequestTest() {
		// When:
		ResponseEntity<ApiErrorV1> resp = handler.handleValidation(new ConstraintViolationException("bad", null));

		// Then:
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resp.getBody().getCode()).isEqualTo("C002");
	}

	@Test
	void shouldMapAuthAndAccessDeniedTest() {
		// When / Then:
		assertThat(handler.handleAuth(new BadCredentialsException("nope")).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(handler.handleAccessDenied(new AccessDeniedException("no")).getStatusCode())
				.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldMapOptimisticLockingFailureToConflictTest() {
		// Given:
		var ex = new ObjectOptimisticLockingFailureException(
				com.aidigital.aionboarding.domain.lesson.entities.Lesson.class, 42L);

		// When:
		ResponseEntity<ApiErrorV1> resp = handler.handleConcurrencyFailure(ex);

		// Then:
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(resp.getBody().getCode()).isEqualTo("C006");
		assertThat(resp.getBody().getMessage()).contains("changed by someone else");
	}

	@Test
	void shouldMapPessimisticLockTimeoutToConflictTest() {
		// Given: a competing transaction held the row lock past the configured wait timeout.
		var ex = new org.springframework.dao.CannotAcquireLockException("could not acquire lock");

		// When:
		ResponseEntity<ApiErrorV1> resp = handler.handleConcurrencyFailure(ex);

		// Then:
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(resp.getBody().getCode()).isEqualTo("C006");
		assertThat(resp.getBody().getMessage()).contains("changed by someone else");
	}

	@Test
	void shouldMapUnknownToInternalServerErrorTest() {
		// When:
		ResponseEntity<ApiErrorV1> resp = handler.handleUnknown(new RuntimeException("boom"));

		// Then:
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resp.getBody().getCode()).isEqualTo("C000");
	}

	@Test
	void shouldMapLazyInitializationToInternalServerErrorAndIncrementRouteTaggedCounterTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GlobalExceptionHandler handlerWithOwnRegistry = new GlobalExceptionHandler(new CurrentTime(), registry);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/lessons/{id}/ask");

		// When:
		ResponseEntity<ApiErrorV1> resp = handlerWithOwnRegistry.handleLazyInitialization(
				new LazyInitializationException("no session"), request);

		// Then:
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resp.getBody().getCode()).isEqualTo("C000");
		assertThat(registry.get("app.errors.lazy_initialization")
				.tag("uri", "/api/v1/lessons/{id}/ask")
				.counter()
				.count()).isEqualTo(1.0);
	}

	@Test
	void shouldTagLazyInitializationCounterAsUnmatchedWhenNoRouteResolvedTest() {
		// Given:
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		GlobalExceptionHandler handlerWithOwnRegistry = new GlobalExceptionHandler(new CurrentTime(), registry);
		MockHttpServletRequest request = new MockHttpServletRequest();

		// When:
		handlerWithOwnRegistry.handleLazyInitialization(new LazyInitializationException("no session"), request);

		// Then:
		assertThat(registry.get("app.errors.lazy_initialization")
				.tag("uri", "UNMATCHED")
				.counter()
				.count()).isEqualTo(1.0);
	}
}
