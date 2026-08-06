# taint-nondeterminism

A generated project whose only purpose is to make the analyzer's
**run-to-run nondeterminism** reproduce in seconds instead of on a 60k-statement
benchmark.

Two runs of the *same analyzer revision* over *this same project* produce
different IFDS fact sets. On Stirling-PDF the effect is 644 of 63,477 statements
(1.01%); here it is small enough to inspect by hand.

## Why it matters

The drift lands in the SARIF `sourceSinkHash`, which baselines and suppressions
key on. The sink half of the hash is stable; the **source** half moves, because
the source endpoint is trace-derived and traces sit downstream of the drifting
fact layer. Result: a finding nobody touched can read as `Fixed` + `New` between
two runs of identical code.

## Mechanism

    concurrency (IR loading AND/OR the IFDS unit dispatcher)
      -> the global quiescence boundary moves     TaintAnalysisUnitRunnerManager.kt  (handleEventProcessed)
      -> a different delayed-analyzer set per resume round
      -> ++factLimit escalates at a different moment   TaintAnalysisUnitRunner.kt  (resumeDelayedAnalyzers)
      -> methods get analysed at different fact depths
      -> a method does or doesn't materialise <static>/*/{...}
      -> different facts -> different summary edges -> different trace
      -> different source endpoint -> different sourceSinkHash

Quiescence is detected by a shared counter reaching exactly zero, so *which*
thread observes zero decides what is in each unit's delayed set at that instant.
It is an order-dependent budget, not a data race — every race probe on the
merge and propagation sites came back with zero hits.

## What each ingredient is for

Nothing here is decorative; removing any one of these stops the reproduction.

| ingredient | why it is needed |
| --- | --- |
| `@RestController` + `@GetMapping` + `@RequestParam` | matches `lib/spring/untrusted-data-source.yaml`, creating the entry point and the taint source |
| `Runtime.getRuntime().exec(...)` | matches `lib/generic/command-injection-sinks.yaml`. **Without a sink the engine propagates no taint at all** — the fact dump is empty and nothing is ever delayed |
| `holder.L1..L6` chained `.get()` | drives `initialFactAp.depth` to 10, past `INITIAL_ALLOWED_FACT_DEPTH = 3`, so analyzers get delayed |
| `public static L1 STATE`, written in every method | produces the `<static>/*/{...}` facts whose exclusion sets are what actually diverge |
| 60 classes across 12 packages, cross-package calls | multiple analysis units, so the global quiescence boundary has something to race over |

## Expected behaviour

Findings: 180. Runs are **expected to disagree with each other** until the
quiescence boundary is made deterministic.

Pinning *both* concurrency knobs makes it fully reproducible — that is the
control that proves the cause:

| IFDS threads | IR parallelism = 1 | IR parallelism = default |
| --- | --- | --- |
| 1 (`taskset -c 0,1`) | deterministic | drifts |
| default | drifts | drifts |

Both layers are independently causal; only pinning both gives identical results.

## Harness note

This project is registered in `projects/repos.yaml` as `kind: local`. Because it
is expected to be unstable, a non-zero added/removed diff here means
"nondeterminism still present", **not** a code regression in the ref under test.
Once the boundary is fixed, this project should become a stable zero-diff
project, and any diff after that *is* a real regression.

## Running it standalone

Measuring the drift directly needs an analyzer built with the counters
(`QuiescenceProbe`, `FactLimitProbe`, `DepthProbe`); see
`scripts/run_nondeterminism_probe.sh`. Without those probes the drift is still
observable the slow way — scan twice and diff the SARIF `sourceSinkHash` values.
