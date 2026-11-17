package org.example.incident;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentSimulationTest {

    @Test
    void startsSeveralDistinctBusinessWorkers() throws Exception {
        try (IncidentSimulation simulation = new IncidentSimulation()) {
            simulation.start();
            Thread.sleep(100);

            List<Thread> workers = simulation.threads();
            Set<String> names = workers.stream().map(Thread::getName).collect(java.util.stream.Collectors.toSet());
            Set<Thread.State> states = workers.stream().map(Thread::getState).collect(java.util.stream.Collectors.toSet());

            assertTrue(workers.size() >= 4);
            assertEquals(workers.size(), names.size());
            assertTrue(workers.stream().noneMatch(Thread::isDaemon));
            assertTrue(states.contains(Thread.State.RUNNABLE));
            assertTrue(states.stream().anyMatch(state -> state == Thread.State.WAITING
                    || state == Thread.State.TIMED_WAITING));
        }
    }

    @Test
    void rejectsSecondStart() {
        try (IncidentSimulation simulation = new IncidentSimulation()) {
            simulation.start();

            assertThrows(IllegalStateException.class, simulation::start);
        }
    }

    @Test
    void closeBeforeStartIsHarmless() {
        IncidentSimulation simulation = new IncidentSimulation();

        assertDoesNotThrow(simulation::close);
    }

    @Test
    void closeStopsEveryWorkerWithinTwoSeconds() {
        IncidentSimulation simulation = new IncidentSimulation();
        simulation.start();
        List<Thread> workers = simulation.threads();

        assertTimeoutPreemptively(Duration.ofSeconds(2), simulation::close);

        assertTrue(workers.stream().noneMatch(Thread::isAlive));
    }

    @Test
    void closeDoesNotMissAnInProgressStart() throws Exception {
        IncidentSimulation simulation = new IncidentSimulation();
        BlockingCounterMap blockingCounters = new BlockingCounterMap();
        replaceCounterMap(simulation, blockingCounters);

        Thread starter = new Thread(simulation::start, "test-starter");
        CountDownLatch closeReturned = new CountDownLatch(1);
        Thread closer = new Thread(() -> {
            simulation.close();
            closeReturned.countDown();
        }, "test-closer");

        starter.start();
        assertTrue(blockingCounters.firstPutEntered.await(1, TimeUnit.SECONDS));
        closer.start();
        closeReturned.await(1, TimeUnit.SECONDS);
        blockingCounters.allowPuts.countDown();
        starter.join(2_000);
        closer.join(2_000);

        boolean workerSurvivedClose = simulation.threads().stream().anyMatch(Thread::isAlive);
        simulation.close();

        assertFalse(workerSurvivedClose);
    }

    @Test
    void oneWorkerHasDominantSustainedActivity() throws Exception {
        try (IncidentSimulation simulation = new IncidentSimulation()) {
            simulation.start();
            Thread.sleep(250);

            List<Long> counts = simulation.workCounts().values().stream()
                    .sorted(Comparator.reverseOrder())
                    .toList();

            assertTrue(counts.size() >= 4);
            assertFalse(counts.isEmpty());
            assertTrue(counts.get(0) > Math.max(1_000, counts.get(1) * 10));
        }
    }

    private static void replaceCounterMap(IncidentSimulation simulation,
                                          ConcurrentHashMap<String, LongAdder> replacement) throws Exception {
        Field field = IncidentSimulation.class.getDeclaredField("workCounters");
        field.setAccessible(true);
        field.set(simulation, replacement);
    }

    private static final class BlockingCounterMap extends ConcurrentHashMap<String, LongAdder> {
        private final AtomicBoolean firstPut = new AtomicBoolean(true);
        private final CountDownLatch firstPutEntered = new CountDownLatch(1);
        private final CountDownLatch allowPuts = new CountDownLatch(1);

        @Override
        public LongAdder put(String key, LongAdder value) {
            if (firstPut.compareAndSet(true, false)) {
                firstPutEntered.countDown();
                try {
                    allowPuts.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while arranging concurrent start/close", interrupted);
                }
            }
            return super.put(key, value);
        }
    }
}
