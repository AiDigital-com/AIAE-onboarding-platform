package com.aidigital.aionboarding.observability;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;

/**
 * Response wrapper that counts bytes written to the body without retaining them, so
 * {@link PerformanceMetricsFilter} can record response payload size with no added heap
 * beyond a single running counter.
 */
class ByteCountingResponseWrapper extends HttpServletResponseWrapper {

    private CountingServletOutputStream countingStream;

    ByteCountingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (countingStream == null) {
            countingStream = new CountingServletOutputStream(getResponse().getOutputStream());
        }
        return countingStream;
    }

    /**
     * Returns the number of response body bytes written so far, or zero if the body was
     * never written through {@link #getOutputStream()}.
     *
     * @return running byte count
     */
    long getByteCount() {
        return countingStream == null ? 0 : countingStream.getByteCount();
    }
}
