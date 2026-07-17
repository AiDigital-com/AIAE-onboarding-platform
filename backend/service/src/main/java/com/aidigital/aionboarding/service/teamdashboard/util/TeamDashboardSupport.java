package com.aidigital.aionboarding.service.teamdashboard.util;

import com.aidigital.aionboarding.service.common.time.CurrentTime;
import com.aidigital.aionboarding.service.teamdashboard.models.TeamDashboardPeriod;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamDashboardSupport {

    public static final List<String> TEAM_COLORS = List.of(
        "#0009DC", "#F0348E", "#42B1CF", "#FF642D", "#884DCC", "#229E5A", "#00B5FF", "#0B0B0B"
    );

    /**
     * Cap on quiz attempts embedded per low-confidence lesson. Aggregate KPIs
     * (attempts/learners/avgScore) are computed over full history regardless of this cap; only the
     * embedded attempt sample used for drill-down display is bounded by it.
     */
    public static final int LOW_CONFIDENCE_ATTEMPT_SAMPLE_SIZE = 5;

    private static final DateTimeFormatter WEEK_LABEL_FORMAT =
        DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);

    private final CurrentTime currentTime;

    /**
     * Resolves a request period code to the supported dashboard period.
     */
    public TeamDashboardPeriod resolvePeriod(String value) {
        if (value == null || value.isBlank()) {
            return TeamDashboardPeriod.MONTH;
        }
        String normalized = value.trim().toLowerCase();
        for (TeamDashboardPeriod period : TeamDashboardPeriod.values()) {
            if (period.code().equals(normalized)) {
                return period;
            }
        }
        return TeamDashboardPeriod.MONTH;
    }

    /**
     * Converts a nullable number to a primitive value with fallback.
     */
    public int toNumber(Number value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return value.intValue();
    }

    /**
     * Converts a nullable number to an integer or null.
     */
    public Integer toNullableNumber(Number value) {
        return value == null ? null : value.intValue();
    }

    /**
     * Formats a UTC timestamp as a dashboard-friendly relative time string.
     */
    public String formatRelativeTime(LocalDateTime value) {
        if (value == null) {
            return "no activity";
        }
        long diffMs = Duration.between(value, currentTime.utcDateTime()).toMillis();
        long minute = 60_000L;
        long hour = 60 * minute;
        long day = 24 * hour;

        if (diffMs < hour) {
            return Math.max(1, Math.round((double) diffMs / minute)) + " min ago";
        }
        if (diffMs < day) {
            return Math.round((double) diffMs / hour) + " h ago";
        }
        if (diffMs < 2 * day) {
            return "yesterday";
        }
        return Math.round((double) diffMs / day) + " d ago";
    }

    /**
     * Formats a week start timestamp for chart labels.
     */
    public String formatWeekLabel(LocalDateTime weekStart) {
        if (weekStart == null) {
            return "";
        }
        return WEEK_LABEL_FORMAT.format(weekStart);
    }

    /**
     * Converts supported array/list values to a filtered string list.
     */
    public List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String[] strings) {
            return Arrays.stream(strings).filter(Objects::nonNull).filter(s -> !s.isBlank()).toList();
        }
        if (value instanceof Object[] objects) {
            return Arrays.stream(objects)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(s -> !s.isBlank())
                .toList();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(s -> !s.isBlank())
                .toList();
        }
        return List.of();
    }

    /**
     * Returns the first item for each non-null extracted id.
     */
    public <T> List<T> uniqById(List<T> items, Function<T, Long> idExtractor) {
        Set<Long> seen = new LinkedHashSet<>();
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (item == null) {
                continue;
            }
            Long id = idExtractor.apply(item);
            if (id == null || seen.contains(id)) {
                continue;
            }
            seen.add(id);
            result.add(item);
        }
        return result;
    }
}
