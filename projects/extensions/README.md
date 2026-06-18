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

The analyzer's `--ruleset` is a `stringArray` (default `[builtin]` per
`opentaint scan --help`). Each value is either:

* The literal **`builtin`** — normally fetched by the CLI from a GitHub
  release tagged `rules/<version>`. The bench tests in-development
  opentaint SHAs whose rule packs are not (yet) released, so the runner
  **intercepts** this sentinel and points the analyzer at the same
  source-tree pack that would have been packaged into the release.
* A path to a YAML file, or a directory of `*.yml` / `*.yaml` files.
  Reported by the CLI under `User ruleset`.

The runner's policy:

* If `scan-flags` contains no `--ruleset`, the runner inserts a default
  pointing at the staged source-tree pack — same effect as `builtin`.
* If `scan-flags` supplies one or more `--ruleset` tokens, they are
  passed verbatim except that every `builtin` value is rewritten to the
  staged source-tree pack's absolute path. The project owns the list.

Example — use `builtin` plus a custom YAML file and a custom directory:

```yaml
- name: spring-petclinic
  git: https://github.com/spring-projects/spring-petclinic.git
  head: 3e1ce239f4488f20abda24441388a515ea55a815
  scan-flags:
    # Rewritten by the runner to `<build-dir>/rules` (the staged pack):
    - --ruleset
    - builtin
    # A single custom YAML file:
    - --ruleset
    - "{ext}/spring-petclinic/rules/sql-injection.yaml"
    # …and a whole directory of `*.yaml` / `*.yml` rule files:
    - --ruleset
    - "{ext}/spring-petclinic/rules"
```

Resulting analyzer command (conceptually):
`--ruleset <build>/rules --ruleset <ext>/.../sql-injection.yaml --ruleset <ext>/.../rules`.

Use `--rule-id` to narrow which rules from those sets actually run.

### Why we rewrite `builtin`

The CLI resolves `--ruleset builtin` by fetching
`https://api.github.com/repos/<org>/<repo>/releases/tags/rules/<version>`.
That path is unusable in the bench for two reasons:

1. The bench analyses in-development SHAs whose rule packs may not be
   published as a release.
2. The current CLI's URL template duplicates the org segment, producing a
   404 (`api.github.com/repos/seqra/seqra/opentaint/…`).

`build_opentaint.sh` copies `opentaint/rules/ruleset` — the same YAMLs that
would have been packaged into the release — into `<build-dir>/rules`.
From the analyzer's perspective the rules are identical; only the CLI's
coverage report labels the pack as `User ruleset` rather than `Bundled`,
which is purely cosmetic.

### The `{rules}` placeholder

`{rules}` is an explicit alias for the same staged pack. After the
`builtin` rewrite described above it is mostly redundant, but it remains
useful in tokens that aren't `--ruleset` values (for example, if a future
flag accepts a rules directory):

```yaml
  scan-flags:
    - --some-future-flag
    - "{rules}/foo.yaml"
```

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

`--ruleset` is **not** reserved. The runner only inserts a default
`--ruleset` pointing at the staged source-tree pack when the project omits
the flag entirely; supplying any `--ruleset` value disables the default.
