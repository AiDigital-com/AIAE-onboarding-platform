package com.aidigital.aionboarding.external.common.http;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Times a single call to a non-HTTP-client external dependency (e.g. the AWS SDK S3 client,
 * which bypasses {@link org.springframework.web.client.RestClient} and so isn't covered by
 * {@link ExternalClientMetricsInterceptor}), recording an {@code external.client.requests}
 * timer tagged by a fixed logical client name, operation, and coarse success/error outcome.
 */
@Component
@RequiredArgsConstructor
public class ExternalCallTimer {

    private final MeterRegistry meterRegistry;

    /**
     * Times {@code call}, recording the outcome as {@code success} if it returns normally or
     * {@code error} if it throws, then rethrowing. The call's own exception handling/mapping is
     * untouched — this only wraps it with a timer.
     *
     * @param client    fixed low-cardinality logical client name (e.g. {@code s3})
     * @param operation fixed low-cardinality operation name (e.g. {@code putObject})
     * @param call      the call to time
     * @param <T>       call result type
     * @return the value returned by {@code call}
     */
    public <T> T record(String client, String operation, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return call.get();
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            sample.stop(Timer.builder("external.client.requests")
                .tag("client", client)
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(meterRegistry));
        }
    }

    /**
     * {@code void}-returning variant of {@link #record(String, String, Supplier)}.
     *
     * @param client    fixed low-cardinality logical client name (e.g. {@code s3})
     * @param operation fixed low-cardinality operation name (e.g. {@code deleteObjects})
     * @param call      the call to time
     */
    public void record(String client, String operation, Runnable call) {
        record(client, operation, () -> {
            call.run();
            return null;
        });
    }
}
