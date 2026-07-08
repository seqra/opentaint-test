#!/usr/bin/env python3
"""Expand projects/repos.yaml into a GitHub Actions matrix.

Produces matrix entries covering (project × ref) pairs. Optional --filter
restricts the project set (substring match against project name). Optional
--misses-only restricts to (project, ref) pairs flagged as cache misses.

A project entry is either:

* ``kind: git`` (the default) — cloned from ``git`` at ``head`` by the
  workflow, then built by the autobuilder (``opentaint compile``).
* ``kind: local`` — a buildable project vendored in this repo at ``source``.
  The workflow skips the clone and points the runner at ``source`` directly;
  ``opentaint compile`` builds it exactly like a cloned project. Local cells
  carry ``git: ""`` and the constant ``head: "local"`` (kit-content changes
  invalidate the cache via the test-system SHA, already part of every key).

Output JSON shape (printed to stdout):

    {"include": [
        {"project": "spring-petclinic", "kind": "git",
         "git": "...", "head": "...", "source": "",
         "java_version": "17", "max_memory": "8G", "compilation_timeout": "1200",
         "ref_kind": "base", "analyzer_sha": "<sha>"},
        ...
    ]}
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import yaml

DEFAULT_JAVA = "17"
DEFAULT_MEMORY = "8G"
DEFAULT_COMPILATION_TIMEOUT = "1200"
DEFAULT_KIND = "git"

# Cache-key head for local projects. A constant (no `/` or space, so it is a
# valid cache_key component); kit-content changes still invalidate the cache
# through the test-system SHA, which is part of every key.
LOCAL_HEAD_SENTINEL = "local"


def _matches_filter(name: str, patterns: list[str]) -> bool:
    if not patterns:
        return True
    return any(p in name for p in patterns)


def _normalise_scan_flags(raw) -> list[str]:
    """Coerce the YAML `scan-flags` field into a clean ``list[str]``.

    Accepts None / missing (→ empty list) or a list of scalars. Any other
    shape is a configuration error and raised loudly so the workflow fails
    fast instead of silently dropping flags.
    """
    if raw is None:
        return []
    if not isinstance(raw, list):
        raise ValueError(
            f"scan-flags must be a list, got {type(raw).__name__}: {raw!r}")
    return [str(token) for token in raw]


def _resolve_identity(repo: dict) -> tuple[str, str, str, str]:
    """Return ``(kind, git, head, source)`` for one repo entry, validating the
    fields required by its kind.

    * ``git`` projects must supply ``git`` and ``head``.
    * ``local`` projects must supply ``source``; they carry ``git: ""`` and the
      constant ``head`` sentinel.

    Raises ``ValueError`` on an unknown kind or a missing required field, so a
    misconfigured entry fails the workflow fast instead of producing a matrix
    cell that breaks further downstream.
    """
    name = repo.get("name", "<unnamed>")
    kind = str(repo.get("kind", DEFAULT_KIND))
    if kind == "git":
        git = repo.get("git")
        head = repo.get("head")
        if not git or not head:
            raise ValueError(
                f"{name}: git project requires both 'git' and 'head'")
        return kind, str(git), str(head), ""
    if kind == "local":
        source = repo.get("source")
        if not source:
            raise ValueError(f"{name}: local project requires 'source'")
        return kind, "", LOCAL_HEAD_SENTINEL, str(source)
    raise ValueError(
        f"{name}: unknown kind {kind!r} (expected 'git' or 'local')")


def _load_misses(path: str | None) -> set[tuple[str, str]]:
    if not path:
        return set()
    data = json.loads(Path(path).read_text())
    return {(e["project"], e["ref_kind"]) for e in data.get("misses", [])}


def build_matrix(repos_path: Path, base_sha: str, new_sha: str,
                 projects_filter: list[str],
                 misses_only: str | None) -> dict:
    data = yaml.safe_load(repos_path.read_text())
    misses = _load_misses(misses_only)
    include: list[dict] = []
    refs = [("base", base_sha), ("new", new_sha)]
    if base_sha == new_sha:
        refs = [("base", base_sha)]

    for repo in data.get("repositories", []):
        name = repo["name"]
        if not _matches_filter(name, projects_filter):
            continue
        kind, git, head, source = _resolve_identity(repo)
        for ref_kind, sha in refs:
            if misses_only and (name, ref_kind) not in misses:
                continue
            include.append({
                "project": name,
                "kind": kind,
                "git": git,
                "head": head,
                "source": source,
                "java_version": str(repo.get("java-version", DEFAULT_JAVA)),
                "max_memory": str(repo.get("max-memory", DEFAULT_MEMORY)),
                "compilation_timeout": str(
                    repo.get("compilation-timeout", DEFAULT_COMPILATION_TIMEOUT)),
                # Emitted as a real JSON array — not a nested JSON-encoded
                # string — so the matrix payload contains no backslash
                # escapes that downstream shell + Python interpolation
                # would otherwise mangle. The workflow re-serialises with
                # ``toJson(matrix.scan_flags)`` at the point of use.
                "scan_flags": _normalise_scan_flags(repo.get("scan-flags")),
                "ref_kind": ref_kind,
                "analyzer_sha": sha,
            })
    return {"include": include}


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--repos", default="projects/repos.yaml", type=Path)
    p.add_argument("--base-sha", required=True)
    p.add_argument("--new-sha", required=True)
    p.add_argument("--filter", default="", help="comma-separated substrings")
    p.add_argument("--misses-only", default=None,
                   help="path to probe output JSON; restrict matrix to misses")
    args = p.parse_args()

    patterns = [s for s in args.filter.split(",") if s]
    matrix = build_matrix(args.repos, args.base_sha, args.new_sha,
                          patterns, args.misses_only)
    json.dump(matrix, sys.stdout)
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
