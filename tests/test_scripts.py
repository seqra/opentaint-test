"""Unit tests for pure-Python helpers.

Run with: python -m pytest new-test/tests -v
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

import pytest

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "scripts"))

import cache_key  # noqa: E402
import compare_sarif  # noqa: E402
import generate_matrix  # noqa: E402
import run_analysis  # noqa: E402


# ── cache_key ────────────────────────────────────────────────────────────────

def test_cache_key_shape():
    k = cache_key.cache_key("aaa", "bbb", "petclinic", "ccc")
    assert k == "sarif-v1-aaa-bbb-petclinic-ccc"


def test_cache_key_rejects_bad_chars():
    with pytest.raises(ValueError):
        cache_key.cache_key("a/b", "x", "y", "z")
    with pytest.raises(ValueError):
        cache_key.cache_key("a", "", "y", "z")


# ── compare_sarif: finding extraction + diff ─────────────────────────────────

def _sarif(findings):
    return {"runs": [{"results": [
        {"ruleId": r, "locations": [{"physicalLocation": {
            "artifactLocation": {"uri": p},
            "region": {"startLine": sl, "endLine": el,
                        "startColumn": sc, "endColumn": ec},
        }}]}
        for (r, p, sl, el, sc, ec) in findings
    ]}]}


def test_diff_no_changes():
    f = [("r1", "a.java", 10, 10, 1, 2)]
    base = compare_sarif._extract_findings(_sarif(f))
    new = compare_sarif._extract_findings(_sarif(f))
    added, removed, unchanged = compare_sarif.diff_findings(base, new, True, False)
    assert added == [] and removed == [] and unchanged == 1


def test_diff_added_and_removed_by_location():
    base = compare_sarif._extract_findings(_sarif([("r1", "a.java", 10, 10, 1, 2)]))
    new = compare_sarif._extract_findings(_sarif([("r1", "a.java", 20, 20, 1, 2)]))
    added, removed, unchanged = compare_sarif.diff_findings(base, new, True, False)
    assert len(added) == 1 and added[0]["startLine"] == 20
    assert len(removed) == 1 and removed[0]["startLine"] == 10
    assert unchanged == 0


def test_diff_rule_only_collapses():
    base = compare_sarif._extract_findings(_sarif([("r1", "a.java", 10, 10, 1, 2)]))
    new = compare_sarif._extract_findings(_sarif([("r1", "b.java", 20, 20, 1, 2)]))
    added, removed, unchanged = compare_sarif.diff_findings(
        base, new, compare_locations=False, compare_columns=False)
    assert added == [] and removed == [] and unchanged == 1


def test_diff_columns_matter_when_enabled():
    base = compare_sarif._extract_findings(_sarif([("r1", "a.java", 10, 10, 1, 2)]))
    new = compare_sarif._extract_findings(_sarif([("r1", "a.java", 10, 10, 5, 6)]))
    a, r, u = compare_sarif.diff_findings(base, new, True, compare_columns=False)
    assert (a, r, u) == ([], [], 1)
    a, r, u = compare_sarif.diff_findings(base, new, True, compare_columns=True)
    assert len(a) == 1 and len(r) == 1 and u == 0


def test_diff_multiset_counts():
    # base has two of the same finding, new has one: one unchanged, one removed.
    f = ("r1", "a.java", 10, 10, 1, 2)
    base = compare_sarif._extract_findings(_sarif([f, f]))
    new = compare_sarif._extract_findings(_sarif([f]))
    a, r, u = compare_sarif.diff_findings(base, new, True, False)
    assert a == [] and len(r) == 1 and u == 1


# ── compare_sarif: bundle-level verdict and status regression ────────────────

def _write_bundle(tmp: Path, sarif_findings, status_tags=None, status="ok", reason=None,
                  scan_seconds=None, peak_memory_bytes=None):
    tmp.mkdir(parents=True, exist_ok=True)
    (tmp / "results.sarif").write_text(json.dumps(_sarif(sarif_findings)))
    (tmp / "status.json").write_text(json.dumps({
        "status": status, "analyzer_status": status_tags, "reason": reason,
        "scan_seconds": scan_seconds, "peak_memory_bytes": peak_memory_bytes,
    }))


def test_status_regression_triggers_fail(tmp_path):
    base = tmp_path / "base"; new = tmp_path / "new"
    _write_bundle(base, [], status_tags=["complete"])
    _write_bundle(new,  [], status_tags=["incomplete", "oom"])
    r = compare_sarif.compare_bundle("proj", base, new, True, False)
    assert r["status_regression"] is True
    assert r["verdict"] == "FAIL"
    assert "status_regression" in r["fail_reasons"]


def test_both_incomplete_no_regression(tmp_path):
    base = tmp_path / "base"; new = tmp_path / "new"
    _write_bundle(base, [], status_tags=["incomplete"])
    _write_bundle(new,  [], status_tags=["incomplete", "oom"])
    r = compare_sarif.compare_bundle("proj", base, new, True, False)
    assert r["status_regression"] is False
    assert r["verdict"] == "PASS"


def test_findings_diff_triggers_fail(tmp_path):
    base = tmp_path / "base"; new = tmp_path / "new"
    _write_bundle(base, [("r1", "a.java", 1, 1, 0, 0)], status_tags=["complete"])
    _write_bundle(new,  [("r1", "a.java", 2, 2, 0, 0)], status_tags=["complete"])
    r = compare_sarif.compare_bundle("proj", base, new, True, False)
    assert r["verdict"] == "FAIL"
    assert r["counts"]["added"] == 1 and r["counts"]["removed"] == 1


def test_scan_error_triggers_fail(tmp_path):
    base = tmp_path / "base"; new = tmp_path / "new"
    _write_bundle(base, [], status_tags=["complete"])
    _write_bundle(new,  [], status_tags=["complete"], status="error", reason="boom")
    r = compare_sarif.compare_bundle("proj", base, new, True, False)
    assert r["verdict"] == "FAIL"
    assert r["new_error"] == "boom"


# ── compare_sarif: time / memory deltas ──────────────────────────────────────

def test_time_memory_deltas_computed(tmp_path):
    base = tmp_path / "base"; new = tmp_path / "new"
    _write_bundle(base, [], status_tags=["complete"],
                  scan_seconds=130.0, peak_memory_bytes=5 * 1024**3)
    _write_bundle(new,  [], status_tags=["complete"],
                  scan_seconds=142.0, peak_memory_bytes=6 * 1024**3)
    r = compare_sarif.compare_bundle("proj", base, new, True, False)
    assert r["time"] == {"base": 130.0, "new": 142.0, "delta": 12.0}
    assert r["memory"] == {"base": 5 * 1024**3, "new": 6 * 1024**3,
                           "delta": 1 * 1024**3}


def test_time_memory_deltas_missing_side(tmp_path):
    base = tmp_path / "base"; new = tmp_path / "new"
    _write_bundle(base, [], status_tags=["complete"],
                  scan_seconds=130.0, peak_memory_bytes=None)
    _write_bundle(new,  [], status_tags=["complete"],
                  scan_seconds=142.0, peak_memory_bytes=6 * 1024**3)
    r = compare_sarif.compare_bundle("proj", base, new, True, False)
    assert r["time"]["delta"] == 12.0
    # Memory missing on base → no delta, base recorded as None.
    assert r["memory"]["base"] is None
    assert r["memory"]["new"] == 6 * 1024**3
    assert r["memory"]["delta"] is None


# ── run_analysis: status extraction ──────────────────────────────────────────

def test_extract_status_complete(tmp_path):
    log = tmp_path / "a.log"
    log.write_text("some text\nAll runners are empty\nmore text\n")
    assert run_analysis.extract_analyzer_status(log) == ["complete"]


def test_extract_status_oom_incomplete(tmp_path):
    log = tmp_path / "a.log"
    log.write_text("Running low on memory, stopping analysis\n")
    assert run_analysis.extract_analyzer_status(log) == ["incomplete", "oom"]


def test_extract_status_all_markers(tmp_path):
    log = tmp_path / "a.log"
    log.write_text(
        "All runners are empty\n"
        "Detected high memory usage\n"
        "Running low on memory, stopping analysis\n"
        "Ifds analysis timeout\n"
    )
    assert run_analysis.extract_analyzer_status(log) == sorted(
        ["complete", "high_memory", "oom", "analysis_timeout"])


def test_extract_status_missing_file(tmp_path):
    assert run_analysis.extract_analyzer_status(tmp_path / "nope.log") is None
    assert run_analysis.extract_analyzer_status(None) is None


# ── run_analysis: peak memory extraction ─────────────────────────────────────

_MEM_LINE = ("2026-05-18 15:21:43 |D| 15:21:43.171 |I| "
             "TaintAnalysisUnitRunnerManager - Memory usage: {used}/30064771072 ({pct})\n")


def test_extract_peak_memory_takes_max(tmp_path):
    log = tmp_path / "a.log"
    log.write_text(
        _MEM_LINE.format(used=21792087928, pct="72,48%")
        + "some unrelated line\n"
        + _MEM_LINE.format(used=29650737400, pct="98,62%")
        + _MEM_LINE.format(used=13708648632, pct="45,60%")
    )
    assert run_analysis.extract_peak_memory(log) == 29650737400


def test_extract_peak_memory_no_lines(tmp_path):
    # Fast / early-terminated run: analyzer never logged a memory sample.
    log = tmp_path / "a.log"
    log.write_text("All runners are empty\n")
    assert run_analysis.extract_peak_memory(log) is None


def test_extract_peak_memory_missing_file(tmp_path):
    assert run_analysis.extract_peak_memory(tmp_path / "nope.log") is None
    assert run_analysis.extract_peak_memory(None) is None


# ── generate_matrix: scan-flags propagation ────────────────────────────

def _write_repos(tmp_path: Path, body: str) -> Path:
    p = tmp_path / "repos.yaml"
    p.write_text(body)
    return p


def test_matrix_scan_flags_default_empty(tmp_path):
    repos = _write_repos(tmp_path, (
        "repositories:\n"
        "  - name: demo\n"
        "    git: https://example.com/demo.git\n"
        "    head: deadbeef\n"
    ))
    m = generate_matrix.build_matrix(repos, "AAA", "BBB", [], None)
    # Real JSON array — not a nested JSON-encoded string — so the matrix
    # payload survives shell + Python interpolation in the workflow.
    assert [e["scan_flags"] for e in m["include"]] == [[], []]


def test_matrix_scan_flags_round_trip(tmp_path):
    repos = _write_repos(tmp_path, (
        "repositories:\n"
        "  - name: demo\n"
        "    git: https://example.com/demo.git\n"
        "    head: deadbeef\n"
        "    scan-flags:\n"
        "      - --rule-id\n"
        "      - java.taint.sql-injection\n"
        "      - \"--passthrough-approximations\"\n"
        "      - \"{ext}/demo/pt.yaml\"\n"
    ))
    m = generate_matrix.build_matrix(repos, "AAA", "AAA", [], None)
    assert len(m["include"]) == 1
    assert m["include"][0]["scan_flags"] == [
        "--rule-id", "java.taint.sql-injection",
        "--passthrough-approximations", "{ext}/demo/pt.yaml",
    ]


def test_matrix_output_has_no_escaped_quotes(tmp_path):
    """Regression: the whole matrix JSON, as printed by main(), must not
    contain ``\\"`` sequences — those break
    ``python -c '... json.loads('''$matrix''') ...'`` in the workflow,
    where Python's source lexer would resolve ``\\"`` to ``"`` before
    JSON parsing."""
    repos = _write_repos(tmp_path, (
        "repositories:\n"
        "  - name: demo\n"
        "    git: https://example.com/demo.git\n"
        "    head: deadbeef\n"
        "    scan-flags:\n"
        "      - --ruleset\n"
        "      - \"{ext}/demo/rules.yaml\"\n"
    ))
    m = generate_matrix.build_matrix(repos, "AAA", "AAA", [], None)
    serialised = json.dumps(m)
    assert "\\\"" not in serialised, (
        f"matrix JSON contains escaped quotes that will break shell+python "
        f"interpolation: {serialised}")


def test_matrix_scan_flags_rejects_non_list(tmp_path):
    repos = _write_repos(tmp_path, (
        "repositories:\n"
        "  - name: demo\n"
        "    git: https://example.com/demo.git\n"
        "    head: deadbeef\n"
        "    scan-flags: --rule-id=foo\n"
    ))
    with pytest.raises(ValueError):
        generate_matrix.build_matrix(repos, "AAA", "AAA", [], None)


# ── run_analysis: scan-flag expansion ────────────────────────────────

def test_expand_scan_flags_substitutes_ext(tmp_path):
    ext = tmp_path / "ext"; ext.mkdir()
    expanded = run_analysis._expand_scan_flags(
        ["--passthrough-approximations", "{ext}/demo/pt.yaml", "--rule-id", "r1"],
        ext,
    )
    assert expanded == [
        "--passthrough-approximations",
        f"{ext.resolve()}/demo/pt.yaml",
        "--rule-id",
        "r1",
    ]


def test_expand_scan_flags_no_placeholder_no_ext_ok():
    # Tokens without {ext} don't require an extensions directory.
    assert run_analysis._expand_scan_flags(["--rule-id", "r1"], None) == [
        "--rule-id", "r1",
    ]


def test_expand_scan_flags_missing_ext_raises():
    with pytest.raises(ValueError):
        run_analysis._expand_scan_flags(["{ext}/demo/pt.yaml"], None)


def test_expand_scan_flags_empty_passthrough():
    assert run_analysis._expand_scan_flags([], None) == []


# ── run_analysis: scan-cmd assembly + ruleset policy ───────────────────

def _scan_cmd(extra_flags, tmp_path):
    return run_analysis._build_scan_cmd(
        opentaint=tmp_path / "opentaint",
        analyzer_jar=tmp_path / "analyzer.jar",
        model_dir=tmp_path / "model",
        sarif=tmp_path / "out.sarif",
        max_memory="8G",
        scan_timeout_seconds=1080,
        extra_flags=extra_flags,
    )


def _rulesets_in(cmd):
    """Return the values that follow every occurrence of '--ruleset' in cmd."""
    return [cmd[i + 1] for i, tok in enumerate(cmd) if tok == "--ruleset"]


def test_build_scan_cmd_default_inserts_builtin_sentinel(tmp_path):
    """With no project --ruleset, the runner must default to ``builtin`` —
    NOT to a filesystem path (which opentaint would classify as a
    'User ruleset')."""
    cmd = _scan_cmd([], tmp_path)
    assert _rulesets_in(cmd) == ["builtin"]


def test_build_scan_cmd_project_ruleset_disables_default(tmp_path):
    """Once the project supplies any --ruleset, the runner adds none of
    its own — the project is in full control and may layer ``builtin``
    plus custom rules in any order."""
    cmd = _scan_cmd(
        ["--ruleset", "/abs/custom-rules.yaml",
         "--ruleset", "/abs/rules-dir"],
        tmp_path,
    )
    assert _rulesets_in(cmd) == [
        "/abs/custom-rules.yaml",
        "/abs/rules-dir",
    ]


def test_build_scan_cmd_project_can_request_builtin_explicitly(tmp_path):
    """Projects may stack ``builtin`` alongside their own rule files."""
    cmd = _scan_cmd(
        ["--ruleset", "builtin", "--ruleset", "/abs/extra.yaml"],
        tmp_path,
    )
    # Exactly what the project asked for — no duplicated `builtin`.
    assert _rulesets_in(cmd) == ["builtin", "/abs/extra.yaml"]


def test_build_scan_cmd_extra_flags_appear_after_reserved(tmp_path):
    cmd = _scan_cmd(["--rule-id", "java.taint.sql-injection"], tmp_path)
    for required in ("--analyzer-jar", "--project-model", "--output",
                      "--timeout", "--max-memory", "--debug", "--experimental"):
        assert required in cmd, f"missing reserved flag {required}"
    assert cmd[-2:] == ["--rule-id", "java.taint.sql-injection"]


def test_build_scan_cmd_end_to_end_through_expand(tmp_path):
    ext = tmp_path / "ext"; ext.mkdir()
    expanded = run_analysis._expand_scan_flags(
        ["--ruleset", "{ext}/proj/rules.yaml",
         "--ruleset", "{ext}/proj/rules"],
        ext,
    )
    cmd = _scan_cmd(expanded, tmp_path)
    # Project supplied --ruleset, so runner must NOT add its own default.
    assert _rulesets_in(cmd) == [
        f"{ext.resolve()}/proj/rules.yaml",
        f"{ext.resolve()}/proj/rules",
    ]


# ── run_analysis: {rules} placeholder ───────────────────────────────

def test_expand_rules_placeholder(tmp_path):
    rules = tmp_path / "rules"; rules.mkdir()
    expanded = run_analysis._expand_scan_flags(
        ["--ruleset", "{rules}"], extensions_dir=None, rules_dir=rules,
    )
    assert expanded == ["--ruleset", str(rules.resolve())]


def test_expand_rules_placeholder_missing_rules_dir_raises():
    with pytest.raises(ValueError):
        run_analysis._expand_scan_flags(
            ["--ruleset", "{rules}"], extensions_dir=None, rules_dir=None,
        )


def test_expand_mixed_placeholders(tmp_path):
    ext = tmp_path / "ext"; ext.mkdir()
    rules = tmp_path / "rules"; rules.mkdir()
    expanded = run_analysis._expand_scan_flags(
        ["--ruleset", "builtin",
         "--ruleset", "{rules}",
         "--ruleset", "{ext}/proj/custom.yaml"],
        extensions_dir=ext, rules_dir=rules,
    )
    assert expanded == [
        "--ruleset", "builtin",
        "--ruleset", str(rules.resolve()),
        "--ruleset", f"{ext.resolve()}/proj/custom.yaml",
    ]


# ── compare_sarif: markdown rendering smoke test ──────────────────────────

def test_render_markdown_smoke():
    md = compare_sarif.render_markdown([
        {"project": "p1", "verdict": "PASS",
         "base_status": ["complete"], "new_status": ["complete"],
         "status_regression": False,
         "counts": {"added": 0, "removed": 0, "unchanged": 42},
         "time": {"base": 130.0, "new": 142.0, "delta": 12.0},
         "memory": {"base": 5 * 1024**3, "new": 6 * 1024**3, "delta": 1 * 1024**3}},
        {"project": "p2", "verdict": "FAIL",
         "base_status": ["complete"], "new_status": ["incomplete"],
         "status_regression": True,
         "counts": {"added": 0, "removed": 0, "unchanged": 3},
         "time": {"base": None, "new": None, "delta": None},
         "memory": {"base": None, "new": None, "delta": None}},
    ])
    assert "1 passed" in md and "1 failed" in md
    assert "❌ p2" in md
    assert "**incomplete**" in md
    assert "=findings" in md
    assert "| 42 |" in md
    # New columns present in header.
    assert "scan time" in md and "peak mem" in md
    # Absolute + signed delta for p1.
    assert "142s (+12s)" in md
    assert "6.0G (+1.0G)" in md
    # No-data rendering for p2 (both sides missing).
    assert "<no data: both>" in md


def test_render_markdown_no_data_one_side():
    md = compare_sarif.render_markdown([
        {"project": "p1", "verdict": "PASS",
         "base_status": ["complete"], "new_status": ["complete"],
         "status_regression": False,
         "counts": {"added": 0, "removed": 0, "unchanged": 1},
         "time": {"base": 100.0, "new": 110.0, "delta": 10.0},
         "memory": {"base": None, "new": 6 * 1024**3, "delta": None}},
    ])
    assert "110s (+10s)" in md
    assert "<no data: base>" in md
