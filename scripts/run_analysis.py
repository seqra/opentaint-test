#!/usr/bin/env python3
"""Run opentaint compile + scan for one project against a staged build.

Writes:
    <results-dir>/results.sarif     — SARIF output (may be partial on error)
    <results-dir>/status.json       — {"status": "ok"|"error", "analyzer_status": [...], "reason": "...",
                                        "scan_seconds": float|null, "peak_memory_bytes": int|null}
    <results-dir>/analyzer.log      — analyzer log copied from opentaint, if found
    <results-dir>/run.log           — our own compile+scan stdout/stderr

Usage:
    run_analysis.py \
        --build-dir build \
        --project-dir /path/to/cloned/project \
        --results-dir results/<project>/<ref>/ \
        --max-memory 8G \
        [--timeout 1200]

Exit codes:
    0  status.json was written, autobuilder (compile step) succeeded.
    2  wrapper-level problem (missing build, bad arguments).
    3  status.json was written, but the autobuilder failed. Distinct so the
       workflow can skip caching and fail the job for an easy GitHub rerun.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path


_LOG_FILE_RE = re.compile(r"Log file:\s*(.+\.log)")
# Analyzer prints a periodic sample like:
#   ... Memory usage: 21792087928/30064771072 (72,48%)
# We take the max of the "used" figure as the run's peak. Absent on runs that
# finish (or fail) before the first sample is logged.
_MEM_USAGE_RE = re.compile(r"Memory usage:\s*(\d+)/\d+")
_COMPLETION_MARKER = "All runners are empty"
_STATUS_MARKERS = [
    ("high_memory", "Detected high memory usage"),
    ("oom", "Running low on memory, stopping analysis"),
    ("analysis_timeout", "Ifds analysis timeout"),
]


def extract_analyzer_status(analyzer_log: Path | None) -> list[str] | None:
    """Parse analyzer log and return sorted list of status tags.

    Returns None if the log is missing.
    """
    if analyzer_log is None or not analyzer_log.is_file():
        return None
    try:
        content = analyzer_log.read_text(errors="replace")
    except OSError:
        return None
    tags = ["complete" if _COMPLETION_MARKER in content else "incomplete"]
    for tag, marker in _STATUS_MARKERS:
        if marker in content:
            tags.append(tag)
    return sorted(tags)


def extract_peak_memory(analyzer_log: Path | None) -> int | None:
    """Return peak resident bytes from the analyzer log, or None.

    None means the log is missing or never logged a memory sample (e.g. the
    run terminated before the first periodic reading).
    """
    if analyzer_log is None or not analyzer_log.is_file():
        return None
    try:
        content = analyzer_log.read_text(errors="replace")
    except OSError:
        return None
    used = [int(m.group(1)) for m in _MEM_USAGE_RE.finditer(content)]
    return max(used) if used else None


def _copy_analyzer_log(stdout: str, dest: Path) -> Path | None:
    m = _LOG_FILE_RE.search(stdout or "")
    if not m:
        return None
    src = Path(m.group(1).strip())
    if not src.is_file():
        return None
    try:
        shutil.copy2(src, dest)
        return dest
    except OSError:
        return None


def _run(cmd: list[str], timeout: int, log_fp) -> tuple[int, str, str, float]:
    log_fp.write(f"\n=== CMD === {' '.join(cmd)}\n")
    log_fp.flush()
    start = time.time()
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        rc, out, err = r.returncode, r.stdout, r.stderr
    except subprocess.TimeoutExpired as exc:
        rc = -1
        out = (exc.stdout or "") if isinstance(exc.stdout, str) else (exc.stdout or b"").decode("utf-8", "replace")
        err = (exc.stderr or "") if isinstance(exc.stderr, str) else (exc.stderr or b"").decode("utf-8", "replace")
    dur = time.time() - start
    log_fp.write(f"=== RC === {rc}  === DURATION === {dur:.1f}s\n")
    log_fp.write(f"=== STDOUT ===\n{out}\n=== STDERR ===\n{err}\n")
    log_fp.flush()
    return rc, out, err, dur


def run_pipeline(build_dir: Path, project_dir: Path, results_dir: Path,
                 max_memory: str, timeout: int) -> dict:
    results_dir.mkdir(parents=True, exist_ok=True)
    opentaint = build_dir / "opentaint"
    analyzer_jar = build_dir / "opentaint-project-analyzer.jar"
    go_ir_server = build_dir / "go-ssa-server"
    autobuilder_jar = build_dir / "opentaint-project-auto-builder.jar"
    rules_dir = build_dir / "rules"

    for p in (opentaint, analyzer_jar, go_ir_server, autobuilder_jar, rules_dir):
        if not p.exists():
            raise FileNotFoundError(f"missing build artifact: {p}")

    # Keep project-model OUTSIDE results_dir so it is never cached or uploaded
    # as part of the per-project result bundle (multi-GB per project otherwise).
    model_dir = results_dir.parent / f"{results_dir.name}-project-model"
    sarif = results_dir / "results.sarif"
    run_log = results_dir / "run.log"
    analyzer_log_dst = results_dir / "analyzer.log"

    # Placeholder experimental flag names — confirm via `opentaint --help --experimental`.
    compile_cmd = [
        str(opentaint), "compile", "--debug",
        "--experimental",
        "--autobuilder-jar", str(autobuilder_jar),
        "--output", str(model_dir),
        str(project_dir),
    ]
    scan_cmd = [
        str(opentaint), "scan", "--debug",
        "--experimental",
        "--analyzer-jar", str(analyzer_jar),
        "--go-server-binary", str(go_ir_server),
        "--ruleset", str(rules_dir),
        "--project-model", str(model_dir),
        "--output", str(sarif),
        "--timeout", f"{max(timeout - 120, 60)}s",
        "--max-memory", max_memory,
    ]

    # Do NOT pre-create model_dir: `opentaint compile --output` may refuse to
    # write into an already-existing directory (or produce inconsistent state).
    status: dict = {"status": "ok", "analyzer_status": None, "reason": None,
                    "autobuilder_failed": False, "scan_seconds": None,
                    "peak_memory_bytes": None}
    try:
        with run_log.open("w") as log_fp:
            rc, out, err, _ = _run(compile_cmd, timeout, log_fp)
            if rc != 0:
                status["status"] = "error"
                status["autobuilder_failed"] = True
                msg = (err or out or "").strip()[:400] or "(no output)"
                status["reason"] = f"compile failed rc={rc}: {msg}"
                return status

            rc, out, err, scan_dur = _run(scan_cmd, timeout, log_fp)
            status["scan_seconds"] = round(scan_dur, 1)
            analyzer_log = _copy_analyzer_log(out, analyzer_log_dst)
            status["analyzer_status"] = extract_analyzer_status(analyzer_log)
            status["peak_memory_bytes"] = extract_peak_memory(analyzer_log)

            sarif_written = sarif.exists() and sarif.stat().st_size > 0
            if rc == 0 or sarif_written:
                if rc != 0 and sarif_written:
                    status["reason"] = f"partial: scan rc={rc}, SARIF written"
            else:
                status["status"] = "error"
                if rc == -1:
                    reason = f"Hard timeout after {timeout}s"
                else:
                    reason = (err or out or "").strip()[:400] or "(no output)"
                status["reason"] = f"scan failed rc={rc}: {reason}"
        return status
    finally:
        # The project-model can be multi-GB; we never need it after scan.
        shutil.rmtree(model_dir, ignore_errors=True)


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--build-dir", required=True, type=Path)
    p.add_argument("--project-dir", required=True, type=Path)
    p.add_argument("--results-dir", required=True, type=Path)
    p.add_argument("--max-memory", default="8G")
    p.add_argument("--timeout", type=int, default=1200)
    args = p.parse_args()

    try:
        status = run_pipeline(args.build_dir, args.project_dir,
                              args.results_dir, args.max_memory, args.timeout)
    except FileNotFoundError as e:
        print(f"run_analysis: {e}", file=sys.stderr)
        return 2

    (args.results_dir / "status.json").write_text(json.dumps(status, indent=2))
    print(json.dumps(status))
    return 3 if status.get("autobuilder_failed") else 0


if __name__ == "__main__":
    sys.exit(main())
