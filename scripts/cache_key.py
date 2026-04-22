#!/usr/bin/env python3
"""Canonical cache key for per-project analysis results.

Key shape:
    sarif-v1-<analyzer_sha>-<test_system_sha>-<project_name>-<project_head>

Bumping SCHEMA_VERSION forces global cache invalidation.
"""

from __future__ import annotations

import argparse
import sys

SCHEMA_VERSION = "v1"


def cache_key(analyzer_sha: str, test_system_sha: str,
              project_name: str, project_head: str) -> str:
    for part in (analyzer_sha, test_system_sha, project_name, project_head):
        if not part or "/" in part or " " in part:
            raise ValueError(f"invalid cache key component: {part!r}")
    return f"sarif-{SCHEMA_VERSION}-{analyzer_sha}-{test_system_sha}-{project_name}-{project_head}"


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--analyzer-sha", required=True)
    p.add_argument("--test-system-sha", required=True)
    p.add_argument("--project-name", required=True)
    p.add_argument("--project-head", required=True)
    args = p.parse_args()
    print(cache_key(args.analyzer_sha, args.test_system_sha,
                    args.project_name, args.project_head))
    return 0


if __name__ == "__main__":
    sys.exit(main())
