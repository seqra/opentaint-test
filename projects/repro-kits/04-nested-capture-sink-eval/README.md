# repro-04 — nested lambda capture through an approximation callback (sink/finding regression)

A minimal, self-contained taint flow that a **newer** OpenTaint analyzer stops reporting: a
`source() → sink()` flow that crosses a nested lambda whose capturing inner lambda is invoked by a
dataflow approximation (`Mono#flatMap`). An older analyzer flags it; the newer one does not.

Reduced from `run.halo.app.extension.router.ExtensionPatchHandler#handle`, where the outer
`flatMap` parameter `jsonPatch` is captured into an inner
`client.getJsonExtension(...).flatMap(jsonExtension -> jsonPatch.apply(...))` — and the CWE‑15
JSON‑Patch mass‑assignment finding disappears on the newer analyzer.

## The differential (four flows, one rule)

`src/main/java/test/NestedCaptureSamples.java` holds four `source → sink` flows. A correct engine
flags all four. Three are controls that flag on both analyzer revisions; only the last regresses.

| flow | inner capturing lambda invoked by | 0.4.3 | dev / 0.4.4 |
| --- | --- | :---: | :---: |
| `pureJavaParamCapturedIntoNestedLambda` | project code (`Runnable#run`), plain local | flags | flags |
| `reactorFlatMapParamUsedDirectly` | — (param used directly) | flags | flags |
| `reactorFlatMapParamCapturedIntoRunnable` | project code (`Runnable#run`) | flags | flags |
| `reactorFlatMapParamCapturedIntoNestedFlatMap` | **an approximation** (`Mono#flatMap`) | flags | **MISS** |

The decisive pair is the last two: identical outer approximation, identical captured parameter,
differing only in whether the inner capturing lambda is invoked by project code or by another
approximation callback. **Only the approximation‑invoked case regresses.**

Verified finding counts (this exact kit, rule `nested-capture-source-to-sink`, `Mono#flatMap`
approximation only):

| analyzer | findings |
| --- | :---: |
| `2026.06.26.4253df6` (0.4.3) | **4** |
| `dev` (`improve-cli-go`) / `2026.07.03.c8de580` (0.4.4) | **3** |

Regression semantics for the bench: while a gap persists on both refs the counts match → `PASS`;
when a ref changes the count (a fix restores the 4th finding, or a regression drops it) →
`added/removed != 0` → `FAIL`, surfacing the change.

## Layout

```
projects/repro-kits/04-nested-capture-sink-eval/   ← the buildable Gradle project (this dir)
  build.gradle            reactor-core as an EXTERNAL dependency (so Mono#flatMap is dropped)
  settings.gradle
  gradlew, gradle/wrapper/…   committed wrapper so the autobuilder self-bootstraps
  src/main/java/test/
    Taint.java            source()/sink() markers
    NestedCaptureSamples.java   the four flows
projects/extensions/repro-04/          ← consumed via the {ext} placeholder
  rules/nested-capture-source-to-sink.yaml   the taint rule
  approximations/src/com/example/approximations/Mono.java   the only custom model
```

The stand‑in "external" library here is **reactor‑core**, entered as a normal Maven dependency so
OpenTaint treats `Mono#flatMap` as external/dropped and re‑models it from the approximation
(`Mono#just` / `Mono#block` are built‑ins). Were reactor vendored as project source it would be
analysed directly and the repro would evaporate.

## Wiring into the bench

This kit is a `kind: local` project, wired into `projects/repos.yaml` via the local-project
mechanism (`scripts/generate_matrix.py`; design:
`docs/superpowers/specs/2026-07-07-repro-kits-ci-design.md`):

```yaml
  - name: repro-04-nested-capture-sink-eval
    kind: local
    source: projects/repro-kits/04-nested-capture-sink-eval
    java-version: 21
    scan-flags:
      - --dataflow-approximations
      - "{ext}/repro-04/approximations/src"
      - --ruleset
      - "{ext}/repro-04/rules"
      - --track-external-methods
```

No `--ruleset builtin` — only the kit's own rule runs, so the finding count is exactly the four
flows and the signal stays clean.

## Reproduce locally

```bash
KIT=projects/repro-kits/04-nested-capture-sink-eval
EXT=projects/extensions/repro-04

# newer analyzer (dev) — 3 findings (misses reactorFlatMapParamCapturedIntoNestedFlatMap)
opentaint compile $KIT -o /tmp/m
opentaint scan --project-model /tmp/m \
  --ruleset $EXT/rules --java-models $EXT/approximations/src \
  -o /tmp/dev.sarif

# older analyzer (0.4.3) — 4 findings (flag names differ: --dataflow-approximations)
npx --yes @seqra/opentaint@0.4.3 compile $KIT -o /tmp/m43
npx --yes @seqra/opentaint@0.4.3 scan --project-model /tmp/m43 \
  --ruleset $EXT/rules --dataflow-approximations $EXT/approximations/src \
  -o /tmp/043.sarif
```

## Mechanism note

On the source flow this was reduced from (Halo `ExtensionPatchHandler`, a CWE‑15 join),
`test rule reachability` showed the taint facts reaching the sink are **byte‑identical** on both
revisions — so the loss there is in the newer engine's **finding / sink‑match evaluation**,
downstream of propagation, not a taint‑propagation drop. That supersedes an earlier "approximation
callback drops closure‑captured taint" hypothesis. Whether this minimal kit regresses by the same
mechanism can be confirmed by diffing its `debug-ifds-fact-reachability.sarif` across the two
revisions; either way the kit's bench value is the observable finding‑count delta, which is
independent of that root‑cause detail.
