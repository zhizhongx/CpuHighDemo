package org.example.incident;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public final class IncidentSimulation implements AutoCloseable {

    private static final List<String> WORKER_NAMES = List.of(
            "order-dispatcher",
            "inventory-sync",
            "pricing-refresh",
            "audit-writer"
    );

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    private final Map<String, LongAdder> workCounters = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> pendingOrders = new ConcurrentLinkedQueue<>();
    private final Object auditMonitor = new Object();
    private volatile List<Thread> workers = List.of();

    public synchronized void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Incident simulation has already been started");
        }

        WORKER_NAMES.forEach(name -> workCounters.put(name, new LongAdder()));
        running.set(true);
        workers = List.of(
                worker(WORKER_NAMES.get(0), this::dispatchOrders),
                worker(WORKER_NAMES.get(1), () -> periodicWork(WORKER_NAMES.get(1), 250)),
                worker(WORKER_NAMES.get(2), () -> periodicWork(WORKER_NAMES.get(2), 500)),
                worker(WORKER_NAMES.get(3), this::awaitAuditEvents)
        );
        workers.forEach(Thread::start);
    }

    private Thread worker(String name, Runnable task) {
        return new Thread(task, name);
    }

    private void dispatchOrders() {
        while (running.get()) {
            Long orderId = pendingOrders.poll();
            if (orderId == null) {
                workCounters.get(Thread.currentThread().getName()).increment();
            }
        }
    }

    private void periodicWork(String workerName, long intervalMillis) {
        while (running.get()) {
            workCounters.get(workerName).increment();
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void awaitAuditEvents() {
        synchronized (auditMonitor) {
            while (running.get()) {
                try {
                    auditMonitor.wait();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    List<Thread> threads() {
        return List.copyOf(workers);
    }

    Map<String, Long> workCounts() {
        Map<String, Long> snapshot = new java.util.HashMap<>();
        workCounters.forEach((name, counter) -> snapshot.put(name, counter.sum()));
        return Map.copyOf(snapshot);
    }

    @Override
    public synchronized void close() {
        if (!started.get() || !running.getAndSet(false)) {
            return;
        }

        synchronized (auditMonitor) {
            auditMonitor.notifyAll();
        }
        workers.forEach(Thread::interrupt);

        long deadline = System.nanoTime() + 2_000_000_000L;
        for (Thread worker : workers) {
            long remainingMillis = Math.max(1, (deadline - System.nanoTime()) / 1_000_000L);
            if (remainingMillis <= 0) {
                break;
            }
            try {
                worker.join(remainingMillis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
