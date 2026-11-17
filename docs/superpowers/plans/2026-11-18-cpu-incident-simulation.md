# CPU Incident Simulation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a safe, repeatable local Java incident in which one of several business-like worker threads persistently consumes one CPU core without revealing the culprit before diagnosis.

**Architecture:** Add an isolated `org.example.incident` package containing a lifecycle-managed simulation and a small main entry point. Worker loops share a stop signal so tests can terminate them; only the main entry waits indefinitely. Keep learner instructions outside the incident source and avoid naming the culprit in user-facing output.

**Tech Stack:** Java 21, Maven, JUnit Jupiter 5.11.4, Windows PowerShell, JDK `jcmd`.

**Spec:** `docs/superpowers/specs/2026-11-18-cpu-incident-simulation-design.md`

## Global Constraints

- Use Java 21 and the existing Maven project.
- Consume at most one logical CPU core persistently.
- Do not use network, Docker, databases, or unbounded memory growth.
- All worker threads must stop through the simulation lifecycle during tests.
- Do not reveal the culprit thread in console output or learner instructions.
- This directory has no `.git`; verification is required, but commit steps are omitted.

## Review Focus

- Calling `start()` twice must not create duplicate worker sets; Task 1 tests rejection of a second start.
- Calling `close()` before `start()` must be harmless; Task 1 tests this lifecycle edge.
- Closing a running simulation must terminate every worker within two seconds; Task 1 tests bounded shutdown.
- The simulation must expose multiple distinct thread states while leaving only one sustained CPU worker; Task 1 tests observable worker diversity and one dominant work counter.
- User-facing startup output must not contain the culprit name; Task 2 tests only stable, non-spoiling startup text.

---

### Task 1: Lifecycle-managed incident simulation

**Files:**
- Create: `src/main/java/org/example/incident/IncidentSimulation.java`
- Test: `src/test/java/org/example/incident/IncidentSimulationTest.java`

**Interfaces:**
- Consumes: Java `Thread`, `AtomicBoolean`, and thread-safe counters only.
- Produces: `IncidentSimulation.start()`, `IncidentSimulation.close()`, package-private `threads()` returning an immutable `List<Thread>`, and package-private `workCounts()` returning an immutable `Map<String, Long>`.

- [x] **Step 1: Write failing lifecycle and behavior tests**

Add tests named `startsSeveralDistinctBusinessWorkers`, `rejectsSecondStart`, `closeBeforeStartIsHarmless`, `closeStopsEveryWorkerWithinTwoSeconds`, and `oneWorkerHasDominantSustainedActivity`. Assert at least four unique non-daemon thread names, second start throws `IllegalStateException`, pre-start close does not throw, all workers terminate after close, and after a short sample the largest work counter is at least ten times the second largest without asserting which name wins.

- [x] **Step 2: Run the focused tests and verify RED**

Run: `mvn -q -Dtest=IncidentSimulationTest test`

Expected: compilation failure because `IncidentSimulation` does not exist.

- [x] **Step 3: Implement the minimal lifecycle and workloads**

Implement `public final class IncidentSimulation implements AutoCloseable`. `start()` creates and starts at least four business-named non-daemon workers with distinct waiting/timed-waiting/runnable behavior. Exactly one workload performs sustained non-blocking work; all loops observe a shared stop signal. `close()` signals stop, interrupts blocking workers, joins each for a bounded interval, and is idempotent.

- [x] **Step 4: Run focused tests and verify GREEN**

Run: `mvn -q -Dtest=IncidentSimulationTest test`

Expected: exit code 0 with all five tests passing.

### Task 2: Non-spoiling exercise entry point

**Files:**
- Create: `src/main/java/org/example/incident/CpuIncidentApplication.java`
- Create: `src/test/java/org/example/incident/CpuIncidentApplicationTest.java`
- Create: `INCIDENT_RUNBOOK.md`

**Interfaces:**
- Consumes: `IncidentSimulation.start()` and `IncidentSimulation.close()`.
- Produces: `CpuIncidentApplication.main(String[] args)` as the IDEA run target and a concise learner runbook that does not expose the root cause.

- [x] **Step 1: Write a failing startup-output test**

Test a package-private `startupMessage(long pid)` method. Assert it includes the process PID, says the simulation is running, and does not contain any worker thread name returned by a started `IncidentSimulation`.

- [x] **Step 2: Run the focused test and verify RED**

Run: `mvn -q -Dtest=CpuIncidentApplicationTest test`

Expected: compilation failure because `CpuIncidentApplication` does not exist.

- [x] **Step 3: Implement the main entry and learner runbook**

`main` starts the simulation, registers a shutdown hook that closes it, prints only the stable startup message and PID, then waits without busy-spinning. `INCIDENT_RUNBOOK.md` identifies the run class, safety/stop instructions, the no-source-inspection rule, and the evidence required for completion; it must not include diagnostic commands or the culprit identity.

- [x] **Step 4: Run focused tests and verify GREEN**

Run: `mvn -q -Dtest=CpuIncidentApplicationTest test`

Expected: exit code 0.

### Task 3: Full verification and diagnostic signal check

**Files:**
- Modify only if verification exposes a defect: files from Tasks 1–2.

**Interfaces:**
- Consumes: compiled simulation entry point and JDK diagnostic tools.
- Produces: evidence that the exercise is safe to run and has a stable, discoverable hot thread without disclosing its identity to the learner.

- [x] **Step 1: Run the complete automated suite**

Run: `mvn -q test`

Expected: exit code 0, including the existing `CpuHighDemoTest` tests.

- [x] **Step 2: Start the incident application in a hidden local process**

Run the compiled `org.example.incident.CpuIncidentApplication`, capture its exact PID, and retain that process handle for cleanup.

- [x] **Step 3: Verify sustained CPU evidence**

Take two per-thread `TotalProcessorTime` samples several seconds apart and verify the same thread has the largest positive delta. Capture three `jcmd <pid> Thread.print` outputs and verify that thread's top application frame remains stable.

- [x] **Step 4: Stop only the captured process and verify cleanup**

Stop the exact process handle in a `finally` block and verify the PID no longer exists.

- [x] **Step 5: Re-run the full suite after runtime verification**

Run: `mvn -q test`

Expected: exit code 0 with no leftover Java incident process.
