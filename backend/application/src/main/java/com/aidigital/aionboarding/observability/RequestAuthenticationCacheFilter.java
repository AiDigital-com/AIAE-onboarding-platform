package com.aidigital.aionboarding.observability;

import com.aidigital.aionboarding.service.common.security.RequestAuthenticationCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Clears {@link RequestAuthenticationCache} at the end of every request so the resolved
 * {@code AppUser}/permission map from one request never leaks into the next request served
 * by the same pooled servlet thread.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class RequestAuthenticationCacheFilter extends OncePerRequestFilter {

    private final RequestAuthenticationCache requestAuthenticationCache;

    /**
     * Delegates to the chain, then clears the request-scoped authentication cache
     * regardless of outcome.
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
        try {
            chain.doFilter(request, response);
        } finally {
            requestAuthenticationCache.clear();
        }
    }
}
