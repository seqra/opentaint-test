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

The workflow summary (`$GITHUB_STEP_SUMMARY`) shows, per project: analyzer status for base and new (`complete` / `incomplete` / `oom` / `analysis_timeout` / `high_memory`), added and removed finding counts, scan time and peak memory for the new run (each with its delta against base, e.g. `412s (+282s)` / `27.6G (+15.7G)`), and a per-project verdict. Scan time is the wall-clock of the scan phase; peak memory is the highest resident usage the analyzer logged — rendered as `<no data: base|new|both>` when a run finished or failed before logging a memory sample. A project fails when:

- the analyzer regressed from `complete` on base to `incomplete` on new, **or**
- added/removed finding counts are non-zero, **or**
- the scan errored on either side.

Full diff detail is available in the `regression-diff` artifact.

## Layout

| Path                              | Purpose                                                       |
| --------------------------------- | ------------------------------------------------------------- |
| `.github/workflows/regression.yaml` | Workflow: resolve → probe → build → analyze → compare.     |
| `projects/repos.yaml`             | Benchmark project list (name, git URL, pinned head, etc.).   |
| `projects/extensions/`            | Files (passthroughs, approximations, custom rules…) referenced by per-project `scan-flags`. |
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

## Per-project `opentaint scan` flags

Each entry in `projects/repos.yaml` may declare a `scan-flags` list whose
tokens are appended verbatim to the `opentaint scan` invocation. Use the
literal substring `{ext}` to reference files shipped in `projects/extensions/`
— the runner substitutes it with that directory's absolute path. Since the
substitution is plain string replacement, the resolved path may point at
either a **file** or a **directory** — whichever the underlying flag accepts
(e.g. `--passthrough-approximations` and `--dataflow-approximations` each
take a single file or a whole directory, and may be repeated):

```yaml
- name: spring-petclinic
  git: https://github.com/spring-projects/spring-petclinic.git
  head: 3e1ce239f4488f20abda24441388a515ea55a815
  scan-flags:
    - --passthrough-approximations              # single YAML file
    - "{ext}/spring-petclinic/passthroughs.yaml"
    - --passthrough-approximations              # …or repeat with a directory
    - "{ext}/spring-petclinic/passthroughs"
    - --dataflow-approximations                 # directory of approximations
    - "{ext}/spring-petclinic/approximations"
    - --rule-id
    - java.taint.sql-injection
```

Flags reserved by the runner (`--analyzer-jar`, `--project-model`,
`--output`, `--timeout`, `--max-memory`, `--debug`, `--experimental`) must
not be repeated here. `--ruleset` is **not** reserved — the runner only
inserts the documented default `--ruleset builtin` when `scan-flags`
contains no `--ruleset` of its own. Supplying any `--ruleset` value puts
the project in full control of which rule packs are loaded and in what
order. Example — stack the JAR-baked `builtin` pack with a custom YAML
file and a whole directory of rules:

```yaml
scan-flags:
  - --ruleset
  - builtin                                          # JAR-baked default pack
  - --ruleset
  - "{ext}/my-project/rules/sql-injection.yaml"     # custom YAML file
  - --ruleset
  - "{ext}/my-project/rules"                        # custom rules directory
```

Two placeholders are expanded inside `scan-flags`:

* `{ext}`    → absolute path of `projects/extensions/`.
* `{rules}`  → absolute path of the opentaint source-tree rule pack staged
  at `<build>/rules`. Use this to layer that pack (which is **not** the
  same as `builtin`) on top of, or instead of, the JAR-baked rules.

See [`projects/extensions/README.md`](projects/extensions/README.md) for
the layout convention and a deeper look at `builtin` vs `{rules}` vs
custom YAMLs.

## Open items

See `test-system-design-plan.md` §10. The exact spelling of the
`opentaint {compile,scan} --experimental --analyzer-jar / --autobuilder-jar`
flags must be confirmed against `opentaint --help --experimental` and
updated in `scripts/run_analysis.py` before the workflow will run green.
