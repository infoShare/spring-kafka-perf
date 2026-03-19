package org.example.springkafkaperf.perf;

public record PerfRunResult(
        int requestedCount,
        int repliedCount,
        int timeoutCount,
        long totalDurationMs,
        double averageRoundTripMs,
        long maxRoundTripMs
) {
}

