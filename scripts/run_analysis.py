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
        [--timeout 1200] \
        [--extensions-dir projects/extensions] \
        [--scan-flags-json '["--rule-id", "java.taint.sql-injection"]']

Extra `opentaint scan` flags may be supplied via ``--scan-flags-json``
(a JSON-encoded list of CLI tokens). Two placeholders are expanded inside
every token before invoking opentaint:

* ``{ext}``    → absolute path of ``--extensions-dir``  (project files
  shipped in this repo, e.g. ``{ext}/conductor/passthrough``).
* ``{rules}``  → absolute path of the opentaint source-tree rule pack
  staged at ``<build-dir>/rules``  (use this only when you want those
  YAMLs layered on top of, or instead of, the analyzer's ``builtin`` pack).

Ruleset handling. In the test bench ``builtin`` is treated as an alias for
the rule pack staged at ``<build-dir>/rules`` (a copy of
``opentaint/rules/ruleset`` made by ``build_opentaint.sh``). The CLI's
native ``--ruleset builtin`` sentinel downloads the pack from a GitHub
release tagged ``rules/<version>`` — unavailable for in-development
opentaint SHAs (and 404s anyway due to a CLI URL bug). The staged copy IS
the built-in pack for the revision under test, so we silently rewrite the
sentinel to its absolute path. Two rules result:

* If ``scan-flags`` contains no ``--ruleset``, the runner inserts
  ``--ruleset <build-dir>/rules`` as the default.
* Each ``--ruleset builtin`` pair in ``scan-flags`` is rewritten to
  ``--ruleset <build-dir>/rules`` before invoking opentaint. All other
  ``--ruleset`` values are passed through verbatim.

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

# Placeholders used inside `scan-flags` entries in repos.yaml. Replaced at
# runtime with the absolute paths of, respectively, the per-project
# extensions directory and the source-tree rule pack staged into the build
# artifact at <build-dir>/rules.
EXT_PLACEHOLDER = "{ext}"
RULES_PLACEHOLDER = "{rules}"

# Sentinel value accepted by `opentaint scan --ruleset`. The CLI resolves it
# by downloading a GitHub release tagged `rules/<version>` — not viable for
# in-development SHAs analysed by this bench (and 404s due to a CLI URL
# bug). The runner intercepts the sentinel and points the analyzer at the
# staged source-tree pack at `<build-dir>/rules` instead. See
# `_resolve_builtin_ruleset`.
BUILTIN_RULESET = "builtin"

# Token (as it appears in argv) that introduces a ruleset value. Used to
# decide whether the project already supplied at least one --ruleset so we
# don't override their choice.
RULESET_FLAG = "--ruleset"


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


def _expand_scan_flags(flags: list[str],
                       extensions_dir: Path | None,
                       rules_dir: Path | None = None) -> list[str]:
    """Substitute ``{ext}`` and ``{rules}`` placeholders in every token.

    * ``{ext}``    → absolute path of ``extensions_dir``.
    * ``{rules}``  → absolute path of ``rules_dir`` (the staged opentaint
      source-tree rule pack at ``<build-dir>/rules``).

    Raises a clear error if a token references a placeholder whose
    backing path was not supplied.
    """
    if not flags:
        return []
    needs_ext = any(EXT_PLACEHOLDER in tok for tok in flags)
    needs_rules = any(RULES_PLACEHOLDER in tok for tok in flags)
    if needs_ext and extensions_dir is None:
        raise ValueError(
            f"scan-flags contains {EXT_PLACEHOLDER!r} but no --extensions-dir "
            "was provided")
    if needs_rules and rules_dir is None:
        raise ValueError(
            f"scan-flags contains {RULES_PLACEHOLDER!r} but no rules "
            "directory is available")
    ext_str = str(extensions_dir.resolve()) if extensions_dir is not None else ""
    rules_str = str(rules_dir.resolve()) if rules_dir is not None else ""
    return [
        tok.replace(EXT_PLACEHOLDER, ext_str).replace(RULES_PLACEHOLDER, rules_str)
        for tok in flags
    ]


def _resolve_builtin_ruleset(flags: list[str], rules_dir: Path) -> list[str]:
    """Rewrite every ``--ruleset builtin`` pair to ``--ruleset <rules_dir>``.

    The opentaint CLI resolves the ``builtin`` sentinel by fetching the rule
    pack from a GitHub release tagged ``rules/<version>``. That download is
    unusable in this bench because:

    * we test in-development opentaint SHAs whose rule packs are not (yet)
      published as releases;
    * the CLI's URL template double-includes the org, producing a 404
      (``api.github.com/repos/seqra/seqra/opentaint/…``).

    The staged ``<rules_dir>`` directory is the same source-tree pack that
    would have been packaged into the release, copied at build time by
    ``build_opentaint.sh``. From the analyzer's perspective the rules are
    identical; only the CLI's coverage report labels the pack as a
    ``User ruleset`` instead of ``Bundled`` — a cosmetic mismatch we accept
    in exchange for working offline and against unreleased SHAs.

    Non-``builtin`` ``--ruleset`` values and unrelated tokens are returned
    unchanged.
    """
    if not flags:
        return []
    rules_path = str(rules_dir.resolve())
    out: list[str] = []
    i = 0
    n = len(flags)
    while i < n:
        tok = flags[i]
        out.append(tok)
        if tok == RULESET_FLAG and i + 1 < n:
            value = flags[i + 1]
            out.append(rules_path if value == BUILTIN_RULESET else value)
            i += 2
        else:
            i += 1
    return out


def _build_scan_cmd(opentaint: Path, analyzer_jar: Path, rules_dir: Path,
                    model_dir: Path, sarif: Path, max_memory: str,
                    scan_timeout_seconds: int,
                    extra_flags: list[str]) -> list[str]:
    """Assemble the ``opentaint scan`` argv.

    Ruleset policy:

    * ``builtin`` everywhere in ``extra_flags`` is first rewritten to the
      absolute path of ``rules_dir`` (see ``_resolve_builtin_ruleset``).
    * If after that rewrite ``extra_flags`` still contains at least one
      ``--ruleset`` token, it is passed through verbatim — the project is
      in full control and may layer the staged pack, custom YAML files
      and directories in any order.
    * Otherwise the runner inserts ``--ruleset <rules_dir>`` as the
      implicit default, matching the spirit of the CLI's
      ``default [builtin]``.
    """
    resolved_flags = _resolve_builtin_ruleset(extra_flags, rules_dir)
    if any(tok == RULESET_FLAG for tok in resolved_flags):
        ruleset_args: list[str] = []
    else:
        ruleset_args = [RULESET_FLAG, str(rules_dir.resolve())]
    return [
        str(opentaint), "scan", "--debug",
        "--experimental",
        "--analyzer-jar", str(analyzer_jar),
        *ruleset_args,
        "--project-model", str(model_dir),
        "--output", str(sarif),
        "--timeout", f"{scan_timeout_seconds}s",
        "--max-memory", max_memory,
        *resolved_flags,
    ]


def run_pipeline(build_dir: Path, project_dir: Path, results_dir: Path,
                 max_memory: str, timeout: int,
                 scan_flags: list[str] | None = None,
                 extensions_dir: Path | None = None) -> dict:
    results_dir.mkdir(parents=True, exist_ok=True)
    opentaint = build_dir / "opentaint"
    analyzer_jar = build_dir / "opentaint-project-analyzer.jar"
    autobuilder_jar = build_dir / "opentaint-project-auto-builder.jar"
    rules_dir = build_dir / "rules"

    for p in (opentaint, analyzer_jar, autobuilder_jar, rules_dir):
        if not p.exists():
            raise FileNotFoundError(f"missing build artifact: {p}")

    expanded_extra_flags = _expand_scan_flags(
        scan_flags or [], extensions_dir, rules_dir=rules_dir)

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
    scan_cmd = _build_scan_cmd(
        opentaint=opentaint,
        analyzer_jar=analyzer_jar,
        rules_dir=rules_dir,
        model_dir=model_dir,
        sarif=sarif,
        max_memory=max_memory,
        scan_timeout_seconds=max(timeout - 120, 60),
        extra_flags=expanded_extra_flags,
    )

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
    p.add_argument("--extensions-dir", type=Path, default=None,
                   help="directory whose contents are reachable via the "
                        "{ext} placeholder in --scan-flags-json")
    p.add_argument("--scan-flags-json", default="[]",
                   help="JSON-encoded list of extra CLI tokens appended to "
                        "'opentaint scan'; each token may use {ext}")
    args = p.parse_args()

    try:
        scan_flags = json.loads(args.scan_flags_json)
        if not isinstance(scan_flags, list) or not all(isinstance(t, str) for t in scan_flags):
            raise ValueError("--scan-flags-json must decode to a list of strings")
    except (json.JSONDecodeError, ValueError) as e:
        print(f"run_analysis: bad --scan-flags-json: {e}", file=sys.stderr)
        return 2

    try:
        status = run_pipeline(args.build_dir, args.project_dir,
                              args.results_dir, args.max_memory, args.timeout,
                              scan_flags=scan_flags,
                              extensions_dir=args.extensions_dir)
    except FileNotFoundError as e:
        print(f"run_analysis: {e}", file=sys.stderr)
        return 2
    except ValueError as e:
        print(f"run_analysis: {e}", file=sys.stderr)
        return 2

    (args.results_dir / "status.json").write_text(json.dumps(status, indent=2))
    print(json.dumps(status))
    return 3 if status.get("autobuilder_failed") else 0


if __name__ == "__main__":
    sys.exit(main())
