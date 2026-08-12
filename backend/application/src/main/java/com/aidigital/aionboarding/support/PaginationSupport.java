package com.aidigital.aionboarding.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Shared, null-tolerant page-request normalisation for controllers whose OpenAPI operation takes
 * separate {@code page}/{@code size} query parameters rather than one request DTO (endpoints
 * that take a request DTO instead resolve pagination through that DTO's own application mapper,
 * e.g. {@code MaterialApiMapper.page(SearchMaterialsV1)}).
 *
 * <p>Deliberately kept off every {@code @Mapper} interface: MapStruct treats any mapper method
 * shaped {@code int foo(Integer)} as a candidate implicit type-conversion for <em>every</em>
 * unrelated {@code Integer -> int} property elsewhere in that mapper (and in mappers that
 * {@code uses} it), which previously produced "ambiguous mapping methods" compile failures on
 * fields with nothing to do with pagination (e.g. {@code totalUsers}, {@code flashcardCount}).
 */
@Component
public class PaginationSupport {

    /** Default page size applied when the caller supplies none. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Largest page size a caller may request on a clamped endpoint. */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Resolves the requested zero-based page index for an unclamped endpoint, defaulting to the
     * first page.
     *
     * @param page the caller-supplied page index, or {@code null}
     * @return the requested page index, defaulting to {@code 0}
     */
    public int page(Integer page) {
        return page == null ? 0 : page;
    }

    /**
     * Resolves the requested page size for an unclamped endpoint, defaulting to
     * {@link #DEFAULT_PAGE_SIZE} without an upper bound.
     *
     * @param size the caller-supplied page size, or {@code null}
     * @return the requested page size, defaulting to {@link #DEFAULT_PAGE_SIZE}
     */
    public int size(Integer size) {
        return size == null ? DEFAULT_PAGE_SIZE : size;
    }

    /**
     * Resolves the requested zero-based page index for a clamped endpoint, defaulting to the
     * first page and rejecting a negative request.
     *
     * @param page the caller-supplied page index, or {@code null}
     * @return a zero-based, non-negative page index
     */
    public int normalizedPageIndex(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    /**
     * Resolves the requested page size for a clamped endpoint, defaulting to
     * {@link #DEFAULT_PAGE_SIZE} and clamping to {@code [1, }{@link #MAX_PAGE_SIZE}{@code ]}.
     *
     * @param size the caller-supplied page size, or {@code null}
     * @return a bounded page size
     */
    public int normalizedPageSize(Integer size) {
        return size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    /**
     * Builds a bounded, name/email-sorted page request, used by user/team candidate listings.
     *
     * @param page the caller-supplied page index, or {@code null}
     * @param size the caller-supplied page size, or {@code null}
     * @return a page request sorted by name then email, case-insensitively
     */
    public Pageable sortedByNameEmail(Integer page, Integer size) {
        return PageRequest.of(normalizedPageIndex(page), normalizedPageSize(size), Sort.by(
                Sort.Order.asc("name").ignoreCase(),
                Sort.Order.asc("email").ignoreCase()
        ));
    }

    /**
     * Builds an unsorted page request for queries that hardcode their own {@code ORDER BY}
     * (e.g. incomplete-first, newest-enrolled-first for My Lessons) rather than a property sort.
     *
     * @param page requested zero-based page index
     * @param size requested page size
     * @return a bounded, unsorted page request
     */
    public Pageable unsortedPageable(Integer page, Integer size) {
        return PageRequest.of(normalizedPageIndex(page), normalizedPageSize(size));
    }
}
