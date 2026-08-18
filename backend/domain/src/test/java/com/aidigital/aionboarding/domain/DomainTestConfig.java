package com.aidigital.aionboarding.domain;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Minimal Spring Boot bootstrap for {@code domain}'s own {@code @DataJpaTest} repository tests.
 * <p>
 * {@code domain} has no {@code @SpringBootApplication} of its own — that lives in
 * {@code backend/application} — so {@code @DataJpaTest} needs one {@code @SpringBootConfiguration}
 * reachable from the test package to bootstrap against. Placed at the {@code domain} package
 * root so Spring Boot's default component/entity/repository scan (rooted at this class's package)
 * covers every entity and repository under it, with no explicit {@code @EntityScan} /
 * {@code @EnableJpaRepositories} needed.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class DomainTestConfig {
}
