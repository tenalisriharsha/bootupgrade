package dev.bootupgrade.samples.securityservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeartbeatJobTest {

    @Test
    void startsAtZero() {
        HeartbeatJob job = new HeartbeatJob();

        assertThat(job.beatCount()).isZero();
    }

    @Test
    void eachBeatIncrementsTheCount() {
        HeartbeatJob job = new HeartbeatJob();

        job.beat();
        job.beat();
        job.beat();

        assertThat(job.beatCount()).isEqualTo(3L);
    }
}
