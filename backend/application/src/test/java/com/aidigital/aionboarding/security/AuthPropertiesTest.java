package com.aidigital.aionboarding.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers {@link AuthProperties}' defaults and its hand-written accessors.
 *
 * <p>Accessor round-trips are normally not worth testing. They are here because
 * these accessors are hand-written rather than Lombok-generated (CR-18), which
 * makes a copy-paste slip — assigning or returning the neighbouring field —
 * a real and silent failure mode. Two of the fields are a JWKS URI and an
 * issuer URI of the same type, so such a swap would still compile and would
 * misconfigure JWT validation at runtime.
 *
 * <p>The defaults matter on their own: {@code AuthStartupValidator} relies on
 * {@code authorizedParties} starting blank, and callers dereference
 * {@code getSso()} without a null check.
 */
class AuthPropertiesTest {

	@Test
	void shouldDefaultAllowedEmailDomainToCompanyDomainTest() {
		// Given: a freshly constructed properties bean, before Spring binds anything
		AuthProperties props = new AuthProperties();

		// When-Then: the company domain is the built-in default
		assertThat(props.getAllowedEmailDomain()).isEqualTo("aidigital.com");
	}

	@Test
	void shouldDefaultAuthorizedPartiesToBlankSoStartupValidationFailsClosedTest() {
		// Given: a freshly constructed properties bean
		AuthProperties props = new AuthProperties();

		// When-Then: blank by default, which is what AuthStartupValidator rejects —
		// an unconfigured deployment must fail startup rather than trust every origin
		assertThat(props.getAuthorizedParties()).isEmpty();
	}

	@Test
	void shouldDefaultSsoToNonNullGroupTest() {
		// Given: a freshly constructed properties bean
		AuthProperties props = new AuthProperties();

		// When-Then: callers read getSso().getIssuerUri() without a null check
		assertThat(props.getSso()).isNotNull();
		assertThat(props.getSso().getIssuerUri()).isNull();
	}

	@Test
	void shouldRoundTripTopLevelPropertiesIndependentlyTest() {
		// Given: distinct values for every top-level property, so a field mix-up
		// in the hand-written accessors cannot pass by coincidence
		AuthProperties props = new AuthProperties();
		AuthProperties.Sso sso = new AuthProperties.Sso();

		// When:
		props.setAllowedEmailDomain("example.com");
		props.setPublishableKey("pk_test_publishable");
		props.setAuthorizedParties("http://localhost:5173,https://app.replit.app");
		props.setSso(sso);

		// Then:
		assertThat(props.getAllowedEmailDomain()).isEqualTo("example.com");
		assertThat(props.getPublishableKey()).isEqualTo("pk_test_publishable");
		assertThat(props.getAuthorizedParties()).isEqualTo("http://localhost:5173,https://app.replit.app");
		assertThat(props.getSso()).isSameAs(sso);
	}

	@Test
	void shouldRoundTripSsoPropertiesIndependentlyTest() {
		// Given: three distinct String values — issuerUri and jwkSetUri are the
		// pair most likely to be swapped by a copy-paste slip
		AuthProperties.Sso sso = new AuthProperties.Sso();

		// When:
		sso.setIssuerUri("https://clerk.example.dev");
		sso.setJwkSetUri("https://clerk.example.dev/.well-known/jwks.json");
		sso.setAudience("aionboarding");

		// Then:
		assertThat(sso.getIssuerUri()).isEqualTo("https://clerk.example.dev");
		assertThat(sso.getJwkSetUri()).isEqualTo("https://clerk.example.dev/.well-known/jwks.json");
		assertThat(sso.getAudience()).isEqualTo("aionboarding");
	}
}
