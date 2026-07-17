package com.aidigital.aionboarding.observability;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

/**
 * Records request/response payload size per route as Micrometer {@link DistributionSummary}s,
 * tagged only by the matched route template and HTTP method (never full paths, IDs, or search
 * terms) — deliberately does not buffer/inspect body content, so it adds no extra allocation
 * beyond a byte counter, unlike a body-caching wrapper.
 * <p>
 * Runs immediately after {@link CorrelationIdFilter} so the matched route pattern (set by
 * {@code DispatcherServlet} during {@code chain.doFilter}) is available once the chain returns.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class PerformanceMetricsFilter extends OncePerRequestFilter {

    private static final String UNMATCHED_ROUTE = "UNMATCHED";

    private final MeterRegistry meterRegistry;

    /**
     * Wraps the response in a byte-counting stream, delegates to the chain, then records
     * request/response size distributions tagged by the matched route template and method.
     *
     * @param request  inbound servlet request
     * @param response outbound servlet response
     * @param chain    filter chain to delegate to
     * @throws ServletException propagated from downstream filters
     * @throws IOException      propagated from downstream filters
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        ByteCountingResponseWrapper wrappedResponse = new ByteCountingResponseWrapper(response);
        try {
            chain.doFilter(request, wrappedResponse);
        } finally {
            String route = resolveRouteTemplate(request);
            String method = request.getMethod();
            requestSizeSummary(route, method).record(Math.max(request.getContentLengthLong(), 0));
            responseSizeSummary(route, method).record(wrappedResponse.getByteCount());
        }
    }

    /**
     * Resolves the matched MVC route template for tagging, falling back to a fixed
     * low-cardinality placeholder for requests that never reach a mapped handler (e.g. 404s).
     *
     * @param request inbound servlet request, after the filter chain has run
     * @return the matched route template, or {@code UNMATCHED}
     */
    String resolveRouteTemplate(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? UNMATCHED_ROUTE : pattern.toString();
    }

    /**
     * Returns the request-size distribution summary for the given route/method pair.
     *
     * @param route  matched route template
     * @param method HTTP method
     * @return registered distribution summary
     */
    DistributionSummary requestSizeSummary(String route, String method) {
        return DistributionSummary.builder("http.server.request.size")
            .baseUnit("bytes")
            .tag("uri", route)
            .tag("method", method)
            .register(meterRegistry);
    }

    /**
     * Returns the response-size distribution summary for the given route/method pair.
     *
     * @param route  matched route template
     * @param method HTTP method
     * @return registered distribution summary
     */
    DistributionSummary responseSizeSummary(String route, String method) {
        return DistributionSummary.builder("http.server.response.size")
            .baseUnit("bytes")
            .tag("uri", route)
            .tag("method", method)
            .register(meterRegistry);
    }
}
