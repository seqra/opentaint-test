#!/bin/bash
# Measure analyzer run-to-run nondeterminism on the taint-nondeterminism project.
#
# Compiles the project once, then runs the analyzer N times against that single
# frozen project model, so the only thing varying between runs is the analyzer's
# own concurrency. Prints one line per run; the run is reproducible iff every
# seqSha is identical.
#
#   JAR=/path/to/opentaint-project-analyzer.jar ./scripts/run_nondeterminism_probe.sh
#
# The JAR must be built from a tree carrying the probe counters (QuiescenceProbe,
# FactLimitProbe, DepthProbe). Without them the script still runs but prints no
# counters, which is indistinguishable from "nothing was delayed" - so check that
# you get output before drawing conclusions.
#
# Env:
#   JAR        analyzer jar (required)
#   MODEL      prebuilt project.yaml; if unset, one is built with `opentaint compile`
#   RULES      ruleset dir (default: the installed java ruleset)
#   N          number of runs (default 5)
#   SERIAL     if 1, pin both concurrency knobs - this is the control and must
#              produce an identical seqSha on every run
set -uo pipefail

HERE=$(cd "$(dirname "$0")/.." && pwd)
PROJECT=$HERE/projects/local/taint-nondeterminism

JAR=${JAR:?set JAR to an instrumented opentaint-project-analyzer.jar}
JAVA=${JAVA:-java}
RULES=${RULES:-$HOME/.opentaint/install/lib/rules/java}
N=${N:-5}
SERIAL=${SERIAL:-0}

if [[ -z "${MODEL:-}" ]]; then
  WORK=$(mktemp -d)
  echo "compiling $PROJECT -> $WORK" >&2
  opentaint compile -o "$WORK" "$PROJECT" >&2 || {
    echo "compile failed; pass MODEL=/path/to/project.yaml to skip this step" >&2
    exit 1
  }
  MODEL=$(find "$WORK" -name project.yaml | head -1)
  [[ -n "$MODEL" ]] || { echo "no project.yaml produced under $WORK" >&2; exit 1; }
fi

PREFIX=""
OPTS=""
if [[ "$SERIAL" == "1" ]]; then
  # availableProcessors() honours CPU affinity, so pinning to 2 CPUs yields
  # nThreads = max(2/2, 1) = 1 for the IFDS dispatcher. IR background loading
  # ignores affinity and needs its own property.
  PREFIX="taskset -c 0,1"
  OPTS="-Dorg.opentaint.ir.background.parallelism=1"
  echo "SERIAL control: both concurrency knobs pinned" >&2
fi

echo "model: $MODEL"
for i in $(seq 1 "$N"); do
  d=$(mktemp -d)
  $PREFIX "$JAVA" -Xmx4g $OPTS \
    -Djdk.util.jar.enableMultiRelease=false \
    -jar "$JAR" \
    --project "$MODEL" \
    --output-dir "$d" --logs-file "$d/analyzer.log" --sarif-file-name report.sarif \
    --semgrep-rule-set "$RULES" > "$d/stdout.txt" 2>&1
  counters=$(grep -ho 'rounds=[0-9]*\|seqSha=[0-9a-f]*\|delayedUnitsSum=[0-9]*\|maxInitialDepth=[0-9]*' \
             "$d/stdout.txt" | tr '\n' ' ')
  findings=$(python3 -c "
import json,sys
try: print(len(json.load(open('$d/report.sarif'))['runs'][0]['results']))
except Exception: print('?')" 2>/dev/null)
  echo "run$i: findings=$findings ${counters:-<no counters: jar lacks probes, or nothing was delayed>}"
  rm -rf "$d"
done
