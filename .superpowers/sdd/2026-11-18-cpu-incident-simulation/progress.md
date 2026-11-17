# SDD ledger — plan: E:\productapp\CpuHighDemo\docs\superpowers\plans\2026-11-18-cpu-incident-simulation.md

Setup Ruling: `E:\productapp\CpuHighDemo` is not a Git repository, so git worktree isolation, commit ranges, and the stock SDD scripts cannot operate. Execute in place with plan-scoped ledger and full test/runtime verification. Cost if wrong: changes are not isolated by Git and must be reviewed by exact file scope.

Pre-flight: Task 1 produces `IncidentSimulation.start()` and `close()` consumed by Task 2; signatures match.

Pre-flight: Task 2 produces `CpuIncidentApplication.main(String[])` consumed by Task 3 runtime verification; signatures match.

Task 1: complete (no Git commit range; tests: `mvn -Dtest=IncidentSimulationTest test` → 5 tests, 0 failures, 0 errors).

Task 2: complete (no Git commit range; tests: `mvn -Dtest=CpuIncidentApplicationTest test` → 1 test, 0 failures, 0 errors).

Task 3: complete (no Git commit range; full suite: 8 tests, 0 failures, 0 errors; runtime: same hottest thread across two CPU intervals, stable application frame in 3/3 dumps, exact process cleanup passed).

Final review Ruling: the stock `review-package` script requires Git history, which this project does not have. Prepare an exact-file review package and use a fresh reviewer against the spec, plan, tests, and implementation. Cost if wrong: reviewer cannot use a diff base and must judge the listed files as a whole.

Final: fixed concurrent `start/close` race — `closeDoesNotMissAnInProgressStart` failed against the original lifecycle, passed after serializing lifecycle transitions; full suite 9/9.

Final: fixed shutdown-bound coverage gap — `closeStopsEveryWorkerWithinTwoSeconds` failed under a temporary 2.5-second mutation, passed after restoring bounded production behavior; full suite 9/9.

Final: minor (deferred): worker threads inherit daemon status instead of explicitly forcing non-daemon; ordinary application entry starts from a non-daemon main thread.

Final: minor (deferred): the shutdown loop's `remainingMillis <= 0` check is unreachable after `Math.max(1, ...)`; current workers cooperate and verified shutdown remains below two seconds.

Final: post-fix runtime verification passed — same hottest thread across consecutive CPU intervals, stable application frame in 3/3 dumps, exact process cleanup.

Final Ruling: retain this plan workspace because the project has no Git history and the ledger/review package are the only durable execution record. Cost if wrong: `.superpowers` scratch remains in the project until the user chooses to initialize Git or remove it.

Final verification: `mvn test` → 9 tests, 0 failures, 0 errors, 0 skipped; no incident simulation process remained.

Integration Ruling: the directory now contains a newly initialized normal Git repository on unborn `master`, with no commits and no remotes; every project file is untracked. Do not stage or commit the user's pre-existing project files without an explicit request. Cost if wrong: work remains uncommitted and cannot be merged or pushed until the user chooses an initial-commit policy.
