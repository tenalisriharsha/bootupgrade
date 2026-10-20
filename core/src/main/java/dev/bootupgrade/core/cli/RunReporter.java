package dev.bootupgrade.core.cli;

import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.model.UpgradeStep;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Collectors;

/** Reports a finished {@link UpgradeRun} to the bootupgrade API ({@code POST /api/runs}). */
public final class RunReporter {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** Builds the JSON body the API's {@code CreateUpgradeRunRequest} expects. */
    public static String toJson(UpgradeRun run) {
        String steps = run.steps().stream().map(RunReporter::stepJson).collect(Collectors.joining(","));
        return "{\"repositoryName\":" + q(run.repositoryName())
                + ",\"sourceJavaVersion\":" + q(run.sourceJavaVersion())
                + ",\"targetJavaVersion\":" + q(run.targetJavaVersion())
                + ",\"sourceSpringBootVersion\":" + q(run.sourceSpringBootVersion())
                + ",\"targetSpringBootVersion\":" + q(run.targetSpringBootVersion())
                + ",\"branchName\":" + q(run.branchName())
                + ",\"status\":" + q(run.status().name())
                + ",\"steps\":[" + steps + "]}";
    }

    private static String stepJson(UpgradeStep s) {
        return "{\"name\":" + q(s.name())
                + ",\"kind\":" + q(s.kind().name())
                + ",\"status\":" + q(s.status().name())
                + ",\"description\":" + q(s.description())
                + ",\"diffSummary\":" + (s.diffSummary() == null ? "null" : q(s.diffSummary()))
                + ",\"startedAt\":" + q(s.startedAt().toString())
                + ",\"finishedAt\":" + (s.finishedAt() == null ? "null" : q(s.finishedAt().toString())) + "}";
    }

    /** JSON string literal with the escapes JSON requires. */
    static String q(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    /** Posts the run; returns the HTTP status code. Throws if the API is unreachable. */
    public int report(String apiBaseUrl, UpgradeRun run) throws IOException, InterruptedException {
        String base = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/runs"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(run)))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
