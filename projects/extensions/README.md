# Project extensions

This directory holds files referenced by the `scan-flags` of projects in
[`../repos.yaml`](../repos.yaml). It is mounted into the analyzer runner as
the **extensions directory** and can contain anything `opentaint scan` knows
how to consume — most commonly:

| Subject                        | Typical `opentaint scan` flag      | Accepts                          |
| ------------------------------ | ---------------------------------- | -------------------------------- |
| Pass-through approximations    | `--passthrough-approximations`     | YAML file **or** directory       |
| Dataflow approximations        | `--dataflow-approximations`        | Class directory **or** Java sources directory |
| Custom YAML rules              | `--ruleset`                        | YAML file **or** directory of `*.yml`/`*.yaml` |
| Rule-id filter                 | `--rule-id`                        | rule-id string (repeatable)      |

All path-valued flags above are *repeatable* and accept either a single file
or a directory — group as many or as few entries under `{ext}/...` as you
like, then point the flag at the file or the enclosing directory.

## Layout convention

Group files by project name to keep things tidy. Either point a flag at one
file, or at a directory and let opentaint pick up everything inside it:

```
projects/extensions/
├── README.md
├── <project-name>/
│   ├── passthroughs/          # ← pass directory to --passthrough-approximations
│   │   ├── jackson.yaml
│   │   └── spring.yaml
│   ├── single-passthrough.yaml # ← or pass one YAML file
│   └── approximations/        # ← pass directory to --dataflow-approximations
│       └── ...
└── shared/
    └── ...
```

## Referencing extension files from `repos.yaml`

Use the literal token `{ext}` inside `scan-flags` — the runner substitutes it
with the absolute path of this directory at analysis time. The substitution
is pure string replacement, so the resolved path can point at a **file** or a
**directory**, whichever the flag accepts:

```yaml
- name: spring-petclinic
  git: https://github.com/spring-projects/spring-petclinic.git
  head: 3e1ce239f4488f20abda24441388a515ea55a815
  scan-flags:
    # Single YAML file:
    - --passthrough-approximations
    - "{ext}/spring-petclinic/single-passthrough.yaml"
    # Whole directory of passthrough YAMLs (also valid):
    - --passthrough-approximations
    - "{ext}/spring-petclinic/passthroughs"
    # Approximation classes / Java sources directory:
    - --dataflow-approximations
    - "{ext}/spring-petclinic/approximations"
    # Plain flags without path arguments work too:
    - --rule-id
    - java.taint.sql-injection
```

The `--passthrough-approximations` and `--dataflow-approximations` flags are
repeatable — add the flag multiple times in `scan-flags` to point at several
files or directories.

Flags that don't reference any extension file (e.g. `--rule-id`,
`--code-flow-limit`) work just as well — they're appended verbatim to the
`opentaint scan` invocation.

## Custom rulesets

The analyzer accepts **multiple** `--ruleset` arguments and merges them. The
runner always passes the built-in ruleset first; any additional `--ruleset`
entries in `scan-flags` are layered on top:

```yaml
- name: spring-petclinic
  git: https://github.com/spring-projects/spring-petclinic.git
  head: 3e1ce239f4488f20abda24441388a515ea55a815
  scan-flags:
    # Add a single custom YAML rules file:
    - --ruleset
    - "{ext}/spring-petclinic/rules/sql-injection.yaml"
    # …and a whole directory of `*.yaml` / `*.yml` rule files:
    - --ruleset
    - "{ext}/spring-petclinic/rules"
```

Resulting analyzer command (conceptually):
`--ruleset <built-in> --ruleset <ext>/.../sql-injection.yaml --ruleset <ext>/.../rules`.

Use `--rule-id` to narrow which rules from those sets are actually run.

## Reserved flags

The runner already sets these and you should **not** repeat them in
`scan-flags`:

- `--analyzer-jar`
- `--project-model`
- `--output`
- `--timeout`
- `--max-memory`
- `--debug`
- `--experimental`

`--ruleset` is **not** reserved — the built-in pack is always supplied, and
additional `--ruleset` entries you add in `scan-flags` are merged with it.
