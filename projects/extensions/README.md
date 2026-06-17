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

The analyzer's `--ruleset` is a `stringArray` whose default is `[builtin]`
(see `opentaint scan --help`). The literal value **`builtin`** is a sentinel
that tells the analyzer to load the rule pack baked into its JAR. Any other
value is treated as a path — a YAML file or a directory of `*.yml` / `*.yaml`
files — and reported as a *User ruleset*.

The runner does **not** force any `--ruleset` flag:

* If `scan-flags` contains no `--ruleset`, the runner inserts the documented
  default `--ruleset builtin` so the JAR-baked pack is used.
* If `scan-flags` supplies one or more `--ruleset` tokens, they are passed
  verbatim and the runner adds none of its own. The project is in full
  control of what gets loaded and in what order.

To layer the built-in pack with your own rules, ask for `builtin` explicitly:

```yaml
- name: spring-petclinic
  git: https://github.com/spring-projects/spring-petclinic.git
  head: 3e1ce239f4488f20abda24441388a515ea55a815
  scan-flags:
    # JAR-baked built-in pack — the literal sentinel, not a path:
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
`--ruleset builtin --ruleset <ext>/.../sql-injection.yaml --ruleset <ext>/.../rules`.

Use `--rule-id` to narrow which rules from those sets are actually run.

### The `{rules}` placeholder — opentaint source-tree rules

The build artifact ships the YAML rule pack from the opentaint source tree
at `<build-dir>/rules`. It is **not** the same as `builtin` (which lives
inside the analyzer JAR). If you want to layer that pack on top of — or
instead of — `builtin`, reference it via the `{rules}` placeholder:

```yaml
  scan-flags:
    - --ruleset
    - builtin           # JAR-baked pack
    - --ruleset
    - "{rules}"          # opentaint source-tree pack staged at <build>/rules
```

The runner expands `{rules}` to the absolute path of `<build-dir>/rules` at
analysis time.

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
`--ruleset builtin` when the project omits the flag entirely; supplying
any `--ruleset` value disables the default.
