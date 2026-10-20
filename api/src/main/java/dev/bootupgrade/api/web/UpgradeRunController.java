package dev.bootupgrade.api.web;

import dev.bootupgrade.api.dto.CreateUpgradeRunRequest;
import dev.bootupgrade.api.dto.SuccessRateSummaryDto;
import dev.bootupgrade.api.dto.UpgradeRunDetailDto;
import dev.bootupgrade.api.dto.UpgradeRunSummaryDto;
import dev.bootupgrade.api.service.UpgradeRunService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
public class UpgradeRunController {

    private final UpgradeRunService service;

    public UpgradeRunController(UpgradeRunService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UpgradeRunDetailDto> createRun(@Valid @RequestBody CreateUpgradeRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRun(request));
    }

    @GetMapping
    public List<UpgradeRunSummaryDto> listRuns() {
        return service.listRuns();
    }

    @GetMapping("/{id}")
    public UpgradeRunDetailDto getRun(@PathVariable String id) {
        return service.getRun(id);
    }

    @GetMapping("/stats/success-rate")
    public SuccessRateSummaryDto successRate() {
        return service.successRate();
    }
}
