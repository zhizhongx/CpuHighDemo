package org.example.incident;

import java.util.concurrent.CountDownLatch;

public final class CpuIncidentApplication {

    private CpuIncidentApplication() {
    }

    public static void main(String[] args) throws InterruptedException {
        IncidentSimulation simulation = new IncidentSimulation();
        simulation.start();

        Runtime.getRuntime().addShutdownHook(new Thread(simulation::close, "incident-shutdown"));
        System.out.println(startupMessage(ProcessHandle.current().pid()));

        new CountDownLatch(1).await();
    }

    static String startupMessage(long pid) {
        return "CPU incident simulation is running. PID=" + pid
                + ". Stop it from IDEA after collecting diagnostic evidence.";
    }
}
