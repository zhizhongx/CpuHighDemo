package org.example.incident;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpuIncidentApplicationTest {

    @Test
    void startupMessageReportsPidWithoutRevealingWorkerNames() {
        long pid = 43_210L;
        String message = CpuIncidentApplication.startupMessage(pid);

        assertTrue(message.contains(Long.toString(pid)));
        assertTrue(message.contains("simulation is running"));

        try (IncidentSimulation simulation = new IncidentSimulation()) {
            simulation.start();
            simulation.threads().stream()
                    .map(Thread::getName)
                    .forEach(name -> assertFalse(message.contains(name)));
        }
    }
}
