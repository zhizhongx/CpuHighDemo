package org.example;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpuHighDemoTest {

    @Test
    void createsWorkerWithDiagnosticThreadName() {
        Thread worker = CpuHighDemo.createWorker();

        assertEquals("cpu-worker", worker.getName());
    }

    @Test
    void workerContinuouslyUpdatesCounter() {
        CpuHighDemo.counter = 0;
        Thread worker = CpuHighDemo.createWorker();
        worker.setDaemon(true);
        worker.start();

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            while (CpuHighDemo.counter == 0) {
                Thread.onSpinWait();
            }
            assertTrue(CpuHighDemo.counter > 0);
        });
    }
}
