// Shared auth constants. SSO-only: the single concern here is which routes
// stay public (the SPA shell, static assets, health/metrics, the OpenAPI
// surface). Everything else requires a valid Clerk Bearer JWT.

package com.aidigital.aionboarding.security;

/**
 * Shared constants for public routes.
 */
public final class AuthConstants {

    private AuthConstants() { }

    /** Clerk JWT claim bound as {@code Authentication#getName()}. */
    public static final String USER_ID_CLAIM = "user_id";

    /** Path patterns that must remain public (no Bearer JWT required). */
    public static final String[] PUBLIC_PATHS = {
        "/",
        "/index.html",
        "/favicon.ico",
        "/assets/**",
        "/error",
        "/*.css",
        "/*.js",
        "/*.png",
        "/*.svg",
        "/login",
        "/login/**",
        "/sign-in",
        "/sign-in/**",
        "/sign-up",
        "/sign-up/**",
        // "/**" is required, not just "/actuator/health": Kubernetes startup,
        // readiness and liveness probes and the ALB target-group health check all
        // call the GROUP paths /actuator/health/readiness and
        // /actuator/health/liveness. The exact-match pattern left those returning
        // 401, so the pod never became Ready and the rollout never completed.
        // Anonymous callers still see only {"status":"UP"} because
        // management.endpoint.health.show-details is when-authorized.
        "/actuator/health/**",
        "/actuator/prometheus",
        "/api/v1/specs/**",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    /** Client-side React routes that must serve the SPA shell on browser reload. */
    public static final String[] PUBLIC_SPA_GET_PATHS = {
        "/admin",
        "/library",
        "/lessons",
        "/lessons/**",
        "/roadmaps",
        "/team-progress",
        "/team-permissions",
        "/teams"
    };
}
