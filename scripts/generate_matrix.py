#!/usr/bin/env python3
"""Expand projects/repos.yaml into a GitHub Actions matrix.

Produces matrix entries covering (project × ref) pairs. Optional --filter
restricts the project set (substring match against project name). Optional
--misses-only restricts to (project, ref) pairs flagged as cache misses.

Output JSON shape (printed to stdout):

    {"include": [
        {"project": "spring-petclinic", "git": "...", "head": "...", "path": "",
         "java_version": "17", "max_memory": "8G", "compilation_timeout": "1200",
         "ref_kind": "base", "analyzer_sha": "<sha>"},
        ...
    ]}

A project is sourced either from an upstream git repo (``git`` + ``head``) or
from an in-repo local directory (``path``). Local entries emit an empty ``git``
and ``head: "local"`` (see ``LOCAL_HEAD``); git entries emit an empty ``path``.
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

# Cache-key `project-head` component for local (in-repo) projects, which have no
# upstream commit. Content changes are already covered by the test-system SHA in
# the cache key (the project lives in this repo), so a constant is sufficient.
# Must be a valid cache-key component (no "/" or whitespace; see cache_key.py).
LOCAL_HEAD = "local"


def _resolve_source(repo: dict) -> tuple[str, str, str]:
    """Return ``(git, head, path)`` for one repo entry.

    A project is sourced EITHER from an upstream git repo (``git`` + ``head``)
    OR from an in-repo local directory (``path``); the two are mutually
    exclusive. Local projects emit an empty ``git`` and the ``LOCAL_HEAD``
    sentinel as ``head``; git projects emit an empty ``path``. Every matrix
    entry therefore carries all three keys, so the workflow can branch on
    whether ``path`` is set.
    """
    name = repo["name"]
    has_path = bool(repo.get("path"))
    has_git = "git" in repo or "head" in repo
    if has_path:
        if has_git:
            raise ValueError(
                f"{name}: 'path' is mutually exclusive with 'git'/'head'")
        return "", LOCAL_HEAD, str(repo["path"])
    if "git" not in repo or "head" not in repo:
        raise ValueError(
            f"{name}: must specify either 'path' or both 'git' and 'head'")
    return str(repo["git"]), str(repo["head"]), ""


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
        git, head, path = _resolve_source(repo)
        for ref_kind, sha in refs:
            if misses_only and (name, ref_kind) not in misses:
                continue
            include.append({
                "project": name,
                "git": git,
                "head": head,
                "path": path,
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
