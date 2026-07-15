#!/usr/bin/env python3
"""Compare two project result bundles (SARIF + status.json) and emit diff.

Per-project inputs (directories):
    <base-dir>/results.sarif
    <base-dir>/status.json
    <new-dir>/results.sarif
    <new-dir>/status.json

Output (written to --output <file.json>):
    {
      "project": "<name>",
      "base_status": [...], "new_status": [...],
      "status_regression": true/false,
      "time":   {"base": s|null, "new": s|null, "delta": s|null},
      "memory": {"base": bytes|null, "new": bytes|null, "delta": bytes|null},
      "base_error": "..." | null, "new_error": "..." | null,
      "added":   [ {ruleId, path, startLine, ...}, ... ],
      "removed": [ ... ],
      "counts":  {"added": N, "removed": M, "added_by_rule": {...}, "removed_by_rule": {...}},
      "verdict": "PASS" | "FAIL",
      "fail_reasons": ["status_regression", "findings_diff", "scan_error"]
    }

See test-system-design-plan.md §7 for the comparison algorithm.
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

CODE_FLOWS_FIELD = "codeFlows"
RESULTS_FIELD = "results"
CODE_FLOWS_DIFF_REASON = "codeflows_diff"
LOC_FIELDS_BASE = ("ruleId", "path", "startLine", "endLine")
LOC_FIELDS_COLS = ("ruleId", "path", "startLine", "endLine", "startColumn", "endColumn")


def _sarif_stats(sarif: dict) -> dict:
    results_count = 0
    code_flows_count = 0
    for run in sarif.get("runs", []) or []:
        for result in run.get(RESULTS_FIELD, []) or []:
            results_count += 1
            code_flows_count += len(result.get(CODE_FLOWS_FIELD) or [])
    return {
        "results": results_count,
        CODE_FLOWS_FIELD: code_flows_count,
    }


def _extract_findings(sarif: dict) -> list[dict]:
    findings: list[dict] = []
    for run in sarif.get("runs", []) or []:
        for res in run.get("results", []) or []:
            rule_id = res.get("ruleId") or (res.get("rule") or {}).get("id")
            code_flows_size = len(res.get("codeFlows") or [])
            locations = res.get("locations") or [{}]
            for loc in locations:
                pl = (loc.get("physicalLocation") or {})
                art = (pl.get("artifactLocation") or {})
                region = (pl.get("region") or {})
                findings.append({
                    "ruleId": rule_id,
                    "path": art.get("uri"),
                    "startLine": region.get("startLine"),
                    "endLine": region.get("endLine"),
                    "startColumn": region.get("startColumn"),
                    "endColumn": region.get("endColumn"),
                    CODE_FLOWS_FIELD: code_flows_size,
                })
    return findings


def _key(f: dict, compare_locations: bool, compare_columns: bool,
         compare_code_flows: bool) -> tuple:
    fields = LOC_FIELDS_COLS if compare_columns else LOC_FIELDS_BASE
    if not compare_locations:
        fields = ("ruleId",)
    if compare_code_flows:
        fields += (CODE_FLOWS_FIELD,)
    return tuple(f.get(k) for k in fields)


def diff_findings(base: list[dict], new: list[dict],
                  compare_locations: bool, compare_columns: bool,
                  compare_code_flows: bool = True
                  ) -> tuple[list[dict], list[dict], int]:
    """Multiset diff. Returns (added, removed, unchanged_count)."""
    base_keyed = [(_key(f, compare_locations, compare_columns, compare_code_flows), f) for f in base]
    new_keyed = [(_key(f, compare_locations, compare_columns, compare_code_flows), f) for f in new]
    base_counts = Counter(k for k, _ in base_keyed)
    new_counts = Counter(k for k, _ in new_keyed)

    unchanged = sum(min(base_counts[k], new_counts.get(k, 0)) for k in base_counts)

    remaining = dict(base_counts)
    added: list[dict] = []
    for k, f in new_keyed:
        if remaining.get(k, 0) > 0:
            remaining[k] -= 1
        else:
            added.append(f)

    remaining = dict(new_counts)
    removed: list[dict] = []
    for k, f in base_keyed:
        if remaining.get(k, 0) > 0:
            remaining[k] -= 1
        else:
            removed.append(f)
    return added, removed, unchanged


def _metric_delta(base, new) -> dict:
    """Build a {base, new, delta} record. delta is None unless both sides exist."""
    delta = (new - base) if (base is not None and new is not None) else None
    return {"base": base, "new": new, "delta": delta}


def _load(p: Path) -> dict:
    if not p.is_file():
        return {}
    try:
        return json.loads(p.read_text())
    except (OSError, json.JSONDecodeError):
        return {}


def compare_bundle(project: str, base_dir: Path, new_dir: Path,
                   compare_locations: bool, compare_columns: bool,
                   compare_code_flows: bool = True) -> dict:
    base_sarif_path = base_dir / "results.sarif"
    new_sarif_path = new_dir / "results.sarif"
    base_sarif = _load(base_sarif_path)
    new_sarif = _load(new_sarif_path)
    base_status = _load(base_dir / "status.json")
    new_status = _load(new_dir / "status.json")

    # "No analysis result" means we could not parse any SARIF runs for that
    # side — typically the scan crashed before writing any findings. We track
    # it separately from "zero findings", which is a valid, clean result.
    base_no_result = not (base_sarif_path.is_file() and base_sarif.get("runs"))
    new_no_result = not (new_sarif_path.is_file() and new_sarif.get("runs"))

    base_tags = base_status.get("analyzer_status") or []
    new_tags = new_status.get("analyzer_status") or []
    base_error = base_status.get("reason") if base_status.get("status") == "error" else None
    new_error = new_status.get("reason") if new_status.get("status") == "error" else None

    time_delta = _metric_delta(base_status.get("scan_seconds"),
                               new_status.get("scan_seconds"))
    memory_delta = _metric_delta(base_status.get("peak_memory_bytes"),
                                 new_status.get("peak_memory_bytes"))

    status_regression = ("complete" in base_tags) and ("complete" not in new_tags) and bool(base_tags)

    base_findings = _extract_findings(base_sarif) if base_sarif else []
    new_findings = _extract_findings(new_sarif) if new_sarif else []
    base_sarif_stats = _sarif_stats(base_sarif) if base_sarif else {"results": 0, CODE_FLOWS_FIELD: 0}
    new_sarif_stats = _sarif_stats(new_sarif) if new_sarif else {"results": 0, CODE_FLOWS_FIELD: 0}
    results_delta = _metric_delta(base_sarif_stats["results"], new_sarif_stats["results"])
    code_flows_delta = _metric_delta(base_sarif_stats[CODE_FLOWS_FIELD], new_sarif_stats[CODE_FLOWS_FIELD])
    codeflows_diff = compare_code_flows and code_flows_delta["delta"] != 0
    added, removed, unchanged = diff_findings(
        base_findings, new_findings, compare_locations, compare_columns, compare_code_flows
    )

    fail_reasons = []
    if status_regression:
        fail_reasons.append("status_regression")
    if added or removed:
        fail_reasons.append("findings_diff")
    elif codeflows_diff:
        fail_reasons.append(CODE_FLOWS_DIFF_REASON)
    if base_error or new_error:
        fail_reasons.append("scan_error")

    return {
        "project": project,
        "base_status": base_tags,
        "new_status": new_tags,
        "base_error": base_error,
        "new_error": new_error,
        "base_no_result": base_no_result,
        "new_no_result": new_no_result,
        "status_regression": status_regression,
        "time": time_delta,
        "memory": memory_delta,
        "sarif_results": results_delta,
        CODE_FLOWS_FIELD: code_flows_delta,
        "added": added,
        "removed": removed,
        "counts": {
            "added": len(added),
            "removed": len(removed),
            "unchanged": unchanged,
            "base_total": len(base_findings),
            "new_total": len(new_findings),
            "added_by_rule": dict(Counter(f["ruleId"] for f in added)),
            "removed_by_rule": dict(Counter(f["ruleId"] for f in removed)),
        },
        "verdict": "FAIL" if fail_reasons else "PASS",
        "fail_reasons": fail_reasons,
    }


def _fmt_no_data(base, new) -> str | None:
    """If either side is missing, return a '<no data: ...>' string, else None."""
    missing = [side for side, v in (("base", base), ("new", new)) if v is None]
    if not missing:
        return None
    return f"<no data: {'both' if len(missing) == 2 else missing[0]}>"


def _fmt_metric(metric: dict, unit: str, scale=1.0, decimals=0) -> str:
    """Render '<new><unit> (<+delta><unit>)' or a '<no data: ...>' marker."""
    base, new, delta = metric.get("base"), metric.get("new"), metric.get("delta")
    no_data = _fmt_no_data(base, new)
    if no_data is not None:
        return no_data
    new_s = f"{new / scale:.{decimals}f}{unit}"
    delta_s = f"{delta / scale:+.{decimals}f}{unit}"
    return f"{new_s} ({delta_s})"


def render_markdown(diffs: list[dict]) -> str:
    degraded_tags = {"incomplete", "oom", "analysis_timeout", "high_memory"}

    def fmt_status(tags: list[str]) -> str:
        if not tags:
            return "—"
        return ", ".join(f"**{t}**" if t in degraded_tags else t for t in tags)

    pass_n = sum(1 for d in diffs if d["verdict"] == "PASS")
    fail_n = len(diffs) - pass_n
    status_deg_n = sum(1 for d in diffs if d["status_regression"])

    no_result_n = sum(1 for d in diffs if d.get("base_no_result") or d.get("new_no_result"))

    lines = [
        f"# Regression test report",
        "",
        f"**{pass_n} passed**, **{fail_n} failed**, "
        f"**{status_deg_n} with analyzer-status regression**, "
        f"**{no_result_n} with no analysis result on at least one side**",
        "",
        "| project | base status | new status | ±results | ±codeFlows | =findings | +findings | −findings | scan time | peak mem | verdict | notes |",
        "|---|---|---|---:|---:|---:|---:|---:|---:|---:|---|---|",
    ]
    for d in sorted(diffs, key=lambda x: (x["verdict"] != "FAIL", x["project"])):
        flag = "❌ " if d["verdict"] == "FAIL" else ""
        unchanged = "—" if (d.get("base_no_result") or d.get("new_no_result")) else d['counts'].get('unchanged', 0)
        added = "—" if d.get("new_no_result") else d['counts']['added']
        removed = "—" if d.get("base_no_result") else d['counts']['removed']
        def _clean(s: str, limit: int = 120) -> str:
            # Collapse whitespace and escape pipes so log output (which often
            # contains '|') does not break the markdown table.
            s = " ".join(s.split())
            if len(s) > limit:
                s = s[:limit] + "…"
            return s.replace("|", "\\|")

        notes: list[str] = []
        if d.get("base_no_result"): notes.append("**no base result**")
        if d.get("new_no_result"):  notes.append("**no new result**")
        if d.get("base_error"):     notes.append(f"base error: {_clean(d['base_error'])}")
        if d.get("new_error"):      notes.append(f"new error: {_clean(d['new_error'])}")
        scan_time = _fmt_metric(d.get("time") or {}, "s")
        peak_mem = _fmt_metric(d.get("memory") or {}, "G", scale=1024**3, decimals=1)
        sarif_results = _fmt_metric(d.get("sarif_results") or {}, "")
        code_flows = _fmt_metric(d.get(CODE_FLOWS_FIELD) or {}, "")
        lines.append(
            f"| {flag}{d['project']} | {fmt_status(d['base_status'])} "
            f"| {fmt_status(d['new_status'])} "
            f"| {sarif_results} | {code_flows} "
            f"| {unchanged} | {added} | {removed} "
            f"| {scan_time} | {peak_mem} "
            f"| {d['verdict']} | {'; '.join(notes)} |"
        )
    return "\n".join(lines) + "\n"


def main() -> int:
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)

    p_one = sub.add_parser("project", help="compare one project bundle")
    p_one.add_argument("--project", required=True)
    p_one.add_argument("--base-dir", required=True, type=Path)
    p_one.add_argument("--new-dir", required=True, type=Path)
    p_one.add_argument("--output", required=True, type=Path)
    p_one.add_argument("--no-compare-locations", action="store_true")
    p_one.add_argument("--compare-columns", action="store_true")
    p_one.add_argument("--no-compare-code-flows", action="store_true")

    p_agg = sub.add_parser("aggregate", help="aggregate per-project diff JSONs into report")
    p_agg.add_argument("--diff-dir", required=True, type=Path)
    p_agg.add_argument("--markdown-out", required=True, type=Path)

    args = p.parse_args()

    if args.cmd == "project":
        if args.no_compare_locations and args.compare_columns:
            print("error: --compare-columns requires location comparison", file=sys.stderr)
            return 2
        result = compare_bundle(
            args.project, args.base_dir, args.new_dir,
            compare_locations=not args.no_compare_locations,
            compare_columns=args.compare_columns,
            compare_code_flows=not args.no_compare_code_flows,
        )
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2))
        return 0

    diffs = [json.loads(p.read_text()) for p in sorted(args.diff_dir.glob("*.json"))]
    args.markdown_out.write_text(render_markdown(diffs))
    return 0


if __name__ == "__main__":
    sys.exit(main())
