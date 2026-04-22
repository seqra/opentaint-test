# Opentaint regression test system

Manually-triggered GitHub Action that compares analysis results from two
opentaint revisions across a fixed set of benchmark projects.

## Trigger

```
gh workflow run regression.yaml \
    --field base_ref=main \
    --field new_ref=my-feature-branch \
    --field compare_locations=true \
    --field compare_columns=false
```

Inputs:

| Input               | Default | Description                                       |
| ------------------- | ------- | ------------------------------------------------- |
| `base_ref`          | —       | Opentaint ref (branch/tag/SHA) for the baseline.  |
| `new_ref`           | —       | Opentaint ref whose results are compared vs base. |
| `compare_locations` | `true`  | If false, findings are matched by `ruleId` only.  |
| `compare_columns`   | `false` | If true, column coordinates join the finding key. |
| `projects_filter`   | `""`    | Comma-separated substrings; restricts projects.   |
| `max_parallel`      | `8`     | Upper bound on concurrent analyze jobs.           |

## Report

The workflow summary (`$GITHUB_STEP_SUMMARY`) shows, per project: analyzer status for base and new (`complete` / `incomplete` / `oom` / `analysis_timeout` / `high_memory`), added and removed finding counts, and a per-project verdict. A project fails when:

- the analyzer regressed from `complete` on base to `incomplete` on new, **or**
- added/removed finding counts are non-zero, **or**
- the scan errored on either side.

Full diff detail is available in the `regression-diff` artifact.

## Layout

| Path                              | Purpose                                                       |
| --------------------------------- | ------------------------------------------------------------- |
| `.github/workflows/regression.yaml` | Workflow: resolve → probe → build → analyze → compare.     |
| `projects/repos.yaml`             | Benchmark project list (name, git URL, pinned head, etc.).   |
| `scripts/build_opentaint.sh`      | Build analyzer + autobuilder JARs and Go CLI from a checkout.|
| `scripts/generate_matrix.py`      | Expand `repos.yaml` into a GH Actions matrix.                |
| `scripts/run_analysis.py`         | Run opentaint `compile` + `scan`, extract analyzer status.   |
| `scripts/compare_sarif.py`        | SARIF diff + status regression + verdict + markdown report.  |
| `scripts/cache_key.py`            | Canonical per-project cache key.                             |
| `tests/`                          | Unit tests for pure-Python logic. Run `python -m pytest tests`. |
| `test-system-design-plan.md`      | Design document (authoritative spec).                        |

## Caching

Per-project results (SARIF + `status.json` + analyzer log) are cached in GitHub
Actions' built-in cache, keyed by:

```
sarif-v1-<analyzer_sha>-<test_system_sha>-<project_name>-<project_head>
```

The test-system SHA (= this repo's commit) is part of the key, so any change
to scripts, workflow, or project list automatically invalidates cached
results. Both successful and failed runs are cached; a subsequent session at
a different analyzer or test-system SHA forces a re-run.

The workflow's `probe` job restores the cache before any build runs — if every
project has a hit for a given opentaint ref, the corresponding `build` job is
skipped entirely.

## Running tests locally

```
cd new-test
python -m pytest tests -v
```

## Open items

See `test-system-design-plan.md` §10. The exact spelling of the
`opentaint {compile,scan} --experimental --analyzer-jar / --autobuilder-jar`
flags must be confirmed against `opentaint --help --experimental` and
updated in `scripts/run_analysis.py` before the workflow will run green.
