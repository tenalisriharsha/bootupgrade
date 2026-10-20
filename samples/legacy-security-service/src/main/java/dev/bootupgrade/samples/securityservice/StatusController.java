package dev.bootupgrade.samples.securityservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/status")
public class StatusController {

    private final HeartbeatJob heartbeatJob;

    public StatusController(HeartbeatJob heartbeatJob) {
        this.heartbeatJob = heartbeatJob;
    }

    @GetMapping("/public")
    public String publicStatus() {
        return "ok";
    }

    @GetMapping("/private")
    public String privateStatus() {
        return "ok";
    }

    @GetMapping("/heartbeat-count")
    public long heartbeatCount() {
        return heartbeatJob.beatCount();
    }
}
