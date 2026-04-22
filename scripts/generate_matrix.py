#!/usr/bin/env python3
"""Expand projects/repos.yaml into a GitHub Actions matrix.

Produces matrix entries covering (project × ref) pairs. Optional --filter
restricts the project set (substring match against project name). Optional
--misses-only restricts to (project, ref) pairs flagged as cache misses.

Output JSON shape (printed to stdout):

    {"include": [
        {"project": "spring-petclinic", "git": "...", "head": "...",
         "java_version": "17", "max_memory": "8G",
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


def _matches_filter(name: str, patterns: list[str]) -> bool:
    if not patterns:
        return True
    return any(p in name for p in patterns)


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
        for ref_kind, sha in refs:
            if misses_only and (name, ref_kind) not in misses:
                continue
            include.append({
                "project": name,
                "git": repo["git"],
                "head": repo["head"],
                "java_version": str(repo.get("java-version", DEFAULT_JAVA)),
                "max_memory": str(repo.get("max-memory", DEFAULT_MEMORY)),
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
