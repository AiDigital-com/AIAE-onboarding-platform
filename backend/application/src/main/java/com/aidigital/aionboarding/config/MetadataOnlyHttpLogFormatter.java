// MetadataOnlyHttpLogFormatter — bounded-metadata Logbook formatter for normal operation.
// Canonical spec: templates/generated-project/observability/logbook-http-logging-rules.md.
// Never reads the request/response body, so large lesson/material payloads are never
// parsed, masked, or serialized just to produce a log line. Paired with WithoutBodyStrategy
// in LogbookConfig so the body is also never buffered in the first place.

package com.aidigital.aionboarding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formats Logbook request/response lines with method, path, status, duration, correlation id,
 * and header-declared content length only — no body.
 */
class MetadataOnlyHttpLogFormatter implements HttpLogFormatter {

    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Formats the request side of the log line.
     *
     * @param precorrelation correlates this request with its eventual response
     * @param request        inbound request (body not read)
     * @return compact JSON metadata line
     * @throws IOException propagated by the underlying JSON writer
     */
    @Override
    public String format(Precorrelation precorrelation, HttpRequest request) throws IOException {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("origin", "local");
        fields.put("type", "request");
        fields.put("correlation", precorrelation.getId());
        fields.put("protocol", request.getProtocolVersion());
        fields.put("method", request.getMethod());
        fields.put("path", request.getPath());
        fields.put("contentLength", request.getHeaders().getFirst(HEADER_CONTENT_LENGTH));
        return objectMapper.writeValueAsString(fields);
    }

    /**
     * Formats the response side of the log line.
     *
     * @param correlation correlates this response with its originating request
     * @param response    outbound response (body not read)
     * @return compact JSON metadata line
     * @throws IOException propagated by the underlying JSON writer
     */
    @Override
    public String format(Correlation correlation, HttpResponse response) throws IOException {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("origin", "local");
        fields.put("type", "response");
        fields.put("correlation", correlation.getId());
        fields.put("durationMs", correlation.getDuration().toMillis());
        fields.put("status", response.getStatus());
        fields.put("contentLength", response.getHeaders().getFirst(HEADER_CONTENT_LENGTH));
        return objectMapper.writeValueAsString(fields);
    }
}
