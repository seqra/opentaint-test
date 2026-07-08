# Repro kit 05 — cross-controller Spring stored-injection gap

## Gap reproduced

OpenTaint models Spring Data **stored injection** (a `save(entity)` taints a
`repositoryContent` slot on the shared repository instance; a later `find*()`
reads it back) — but on a real `opentaint scan` the save→find link is
established **only when the save and the find are reached from the SAME
`@RestController`**. When the store happens in one controller and the read-back
happens in a **different** controller, the read-back entity stays untainted and
the downstream sink is a **false negative**.

This is the shape of a real stored SSRF (a controller persists an
attacker-supplied URL; a different controller reads it back and issues an
outbound request to it) that `opentaint scan` fails to surface.

## What the kit contains

All three flows use the same built-in source (Spring `@RequestBody`) and the
same built-in sink (`java-ssrf-sink`: `new URL(x).openStream()`), so **no
extensions or custom rules are needed**.

| Flow | Controllers | Expected today |
| --- | --- | --- |
| `DirectController#body` | one, same request | **finding** (first-order control) |
| `SameController` (`store` + `fetch`) | one controller, two endpoints | **finding** (same-controller stored) |
| `CrossStore#store` → `CrossFetch#fetch` | **two** controllers | **NO finding** (the gap) |

`Note`/`NoteRepo` back the same-controller flow; `Note2`/`Note2Repo` back the
cross-controller flow — structurally identical, differing only in which
controller holds the store vs the read.

## Regression signal

On both refs the gap persists → `DirectController` + `SameController` fire, the
cross-controller flow does not → identical count → **PASS**. When a `new_ref`
fixes the cross-controller stored-injection link, `CrossFetch#fetch` becomes a
finding → `added != 0` → **FAIL**, surfacing the fix (and symmetrically a
regression).

## Manual repro

    opentaint compile . -o /tmp/model
    opentaint scan --project-model /tmp/model --ruleset builtin -o /tmp/out.sarif
    # -> ssrf at DirectController.java and SameController.java ONLY;
    #    CrossFetch.java is absent (the false negative).
