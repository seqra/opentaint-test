#!/usr/bin/env python3
"""Run opentaint compile + scan for one project against a staged build.

Writes:
    <results-dir>/results.sarif     — SARIF output (may be partial on error)
    <results-dir>/status.json       — {"status": "ok"|"error", "analyzer_status": [...], "reason": "..."}
    <results-dir>/analyzer.log      — analyzer log copied from opentaint, if found
    <results-dir>/run.log           — our own compile+scan stdout/stderr

Usage:
    run_analysis.py \
        --build-dir build \
        --project-dir /path/to/cloned/project \
        --results-dir results/<project>/<ref>/ \
        --max-memory 8G \
        [--timeout 1200]

Exit code is 0 if status.json was written (even on analyzer error); non-zero
only on wrapper-level problems (missing build, bad arguments).
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


def _run(cmd: list[str], timeout: int, log_fp) -> tuple[int, str, str]:
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
    return rc, out, err


def run_pipeline(build_dir: Path, project_dir: Path, results_dir: Path,
                 max_memory: str, timeout: int) -> dict:
    results_dir.mkdir(parents=True, exist_ok=True)
    opentaint = build_dir / "opentaint"
    analyzer_jar = build_dir / "opentaint-project-analyzer.jar"
    autobuilder_jar = build_dir / "opentaint-project-auto-builder.jar"
    rules_dir = build_dir / "rules"

    for p in (opentaint, analyzer_jar, autobuilder_jar, rules_dir):
        if not p.exists():
            raise FileNotFoundError(f"missing build artifact: {p}")

    model_dir = results_dir / "project-model"
    sarif = results_dir / "results.sarif"
    run_log = results_dir / "run.log"
    analyzer_log_dst = results_dir / "analyzer.log"

    # Placeholder experimental flag names — confirm via `opentaint --help --experimental`.
    compile_cmd = [
        str(opentaint), "compile", "--quiet", "--debug",
        "--experimental",
        "--autobuilder-jar", str(autobuilder_jar),
        "--output", str(model_dir),
        str(project_dir),
    ]
    scan_cmd = [
        str(opentaint), "scan", "--quiet", "--debug",
        "--experimental",
        "--analyzer-jar", str(analyzer_jar),
        "--ruleset", str(rules_dir),
        "--project-model", str(model_dir),
        "--output", str(sarif),
        "--timeout", f"{max(timeout - 120, 60)}s",
        "--max-memory", max_memory,
    ]

    status: dict = {"status": "ok", "analyzer_status": None, "reason": None}
    with run_log.open("w") as log_fp:
        rc, _out, err = _run(compile_cmd, timeout, log_fp)
        if rc != 0:
            status["status"] = "error"
            status["reason"] = f"compile failed rc={rc}: {(err or '').strip()[:400]}"
            return status

        rc, out, err = _run(scan_cmd, timeout, log_fp)
        analyzer_log = _copy_analyzer_log(out, analyzer_log_dst)
        status["analyzer_status"] = extract_analyzer_status(analyzer_log)

        sarif_written = sarif.exists() and sarif.stat().st_size > 0
        if rc == 0 or sarif_written:
            if rc != 0 and sarif_written:
                status["reason"] = f"partial: scan rc={rc}, SARIF written"
        else:
            status["status"] = "error"
            reason = f"Hard timeout after {timeout}s" if rc == -1 else (err or "").strip()[:400]
            status["reason"] = f"scan failed rc={rc}: {reason}"
    return status


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
    return 0


if __name__ == "__main__":
    sys.exit(main())
