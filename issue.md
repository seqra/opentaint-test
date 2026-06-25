# Vert.x verticle `start()` defined in an ABSTRACT base class is not auto-discovered as an entry point

**Engine:** OpenTaint v0.4.2 (analyzer build 2026.06.24.7562cdf)
**Status:** Real finding silently dropped — codex finding 4 (credentialed CORS reflection). Minimal repro included.
**Note:** This supersedes the earlier "inline handler-lambda / nested-capture" theory for CORS — that theory is
DISPROVEN by the repro below (the nested `addHeadersEndHandler` pattern fires fine). The handler-lambda
reachability gap itself IS fixed by the dataflow approximations in `.opentaint/dataflow/vertx-handlers/` +
`.opentaint/dataflow/kotlinx-coroutines-launch/` (which recovered codex findings 3,5,6,9).

## The exact problem

Entry-point auto-discovery reaches lifecycle/public methods defined in the **concrete** class, but NOT methods
inherited from an **abstract base class**. The reader app puts its CORS handler in the verticle's
`override suspend fun start()` — and that `start()` is defined in the ABSTRACT base `RestVerticle`, while the
deployed verticle `YueduApi : RestVerticle()` does not override it. So `RestVerticle.start()` (with the CORS
handler) is never auto-discovered as an entry for the concrete `YueduApi`, its body is never analyzed, and the
`Origin` → `Access-Control-Allow-Origin` flow never fires. `RestVerticle` appears in NO codeflow in the main scan.

```kotlin
abstract class RestVerticle : CoroutineVerticle() {
    override suspend fun start() {                 // <-- CORS handler lives here, in the ABSTRACT base
        router.route().handler {
            it.addHeadersEndHandler { _ ->
                val origin = it.request().getHeader("Origin")
                it.response().putHeader("Access-Control-Allow-Origin", origin)
            }
        }
        initRouter(router)                          // YueduApi.initRouter — reached as a root, findings fire
    }
    abstract suspend fun initRouter(router: Router)
}
class YueduApi : RestVerticle() { override suspend fun initRouter(router: Router){ ... } }
```

## Reproduction (built + scanned)

`.opentaint/test-projects/getworkdir-repro/` (Vert.x 3.8.1 + vertx-lang-kotlin-coroutines), scanned with the real
rules (`vertx-untrusted-data-source` getHeader source + `vertx-cors-reflection-sink` putHeader sink + the
`csrf-vertx-cors-reflection-lib-ext` join) and the handler/launch dataflow approximations:

| sample | where the CORS handler lives | CORS fires? |
|---|---|---|
| `VertxCors.plainCors` | plain method `fun plainCors(ctx)` | ✅ |
| `VertxCors.directCors` | `route().handler { ctx -> getHeader→putHeader }` | ✅ |
| `VertxCors.nestedCors` | `route().handler { ctx -> ctx.addHeadersEndHandler { getHeader→putHeader } }` (the EXACT codex nested shape) | ✅ |
| `ConcreteCorsVerticle.start` | `override suspend fun start()` in a **concrete** `CoroutineVerticle` | ✅ |
| `CorsVerticleBase.start` (via `MyVerticle`) | `override suspend fun start()` in an **abstract base**, concrete subclass does not override | ❌ |
| `MyVerticle.initRouter` | `getParam→sendFile` in the concrete override of `initRouter` | ✅ (proves the concrete subclass IS analyzed) |

So: the CORS rule, the source/sink, the handler approximations, and even the nested `addHeadersEndHandler`
pattern all WORK (every plain-method and concrete-`start()` case fires). The ONLY non-firing case is
`start()` inherited from the abstract base — exactly the `RestVerticle` shape.

## What it is NOT (ruled out by test)

- NOT the inline handler lambda / `Route.handler` modeling: covered by the dataflow approximations; `directCors`
  and `ConcreteCorsVerticle.start` fire.
- NOT the nested `addHeadersEndHandler` capture: `nestedCors` fires.
- NOT the `getHeader`/`putHeader` source/sink rules: they fire in every reachable case.
- NOT coroutine/`suspend` dispatch per se: `initRouter` is `suspend` and reached; `ConcreteCorsVerticle.start` is
  `suspend` and reached.

## No local workaround

`opentaint scan` has no `--entry-points`/roots flag (only `opentaint test rule reachability` does). There is no
rule/approximation lever to force a method to be an analysis root. So finding 4 cannot be recovered without an
engine change.

## Ask

Include methods inherited from abstract (non-`abstract`-method) base classes — at minimum framework lifecycle
entries such as `io.vertx.core.AbstractVerticle#start` / `io.vertx.kotlin.coroutines.CoroutineVerticle#start`
defined on a base class — in entry-point auto-discovery for the concrete deployed subclass. Repro:
`.opentaint/test-projects/getworkdir-repro/src/main/kotlin/test/{VertxCors,CorsVerticle}.kt`.
