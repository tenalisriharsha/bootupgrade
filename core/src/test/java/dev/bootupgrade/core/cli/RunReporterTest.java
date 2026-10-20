package dev.bootupgrade.core.cli;

import com.sun.net.httpserver.HttpServer;
import dev.bootupgrade.core.model.RunStatus;
import dev.bootupgrade.core.model.StepKind;
import dev.bootupgrade.core.model.UpgradeRun;
import dev.bootupgrade.core.model.UpgradeStep;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RunReporterTest {

    private static UpgradeRun sampleRun() {
        UpgradeStep step = UpgradeStep.start("s1", "Apply \"recipes\"", StepKind.RECIPE, "line one\nline two")
                .markSucceeded("2 file(s) changed: pom.xml, A.java");
        return UpgradeRun.start("run-1", "demo", "8", "21", "2.7.18", "3.3.5", "bootupgrade/demo-1")
                .withStep(step)
                .withStatus(RunStatus.SUCCEEDED);
    }

    @Test
    void jsonMatchesTheApiContractAndEscapesStrings() {
        String json = RunReporter.toJson(sampleRun());

        assertThat(json)
                .contains("\"repositoryName\":\"demo\"")
                .contains("\"status\":\"SUCCEEDED\"")
                .contains("\"kind\":\"RECIPE\"")
                .contains("\"name\":\"Apply \\\"recipes\\\"\"")
                .contains("\"description\":\"line one\\nline two\"")
                .contains("\"startedAt\":\"");
    }

    @Test
    void postsTheRunToTheRunsEndpoint() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            UpgradeRun run = sampleRun();
            int status = new RunReporter().report("http://127.0.0.1:" + server.getAddress().getPort() + "/", run);

            assertThat(status).isEqualTo(201);
            assertThat(path.get()).isEqualTo("POST /api/runs");
            assertThat(body.get()).isEqualTo(RunReporter.toJson(run));
        } finally {
            server.stop(0);
        }
    }
}
