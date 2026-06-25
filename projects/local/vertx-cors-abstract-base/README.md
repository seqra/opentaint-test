# vertx-cors-abstract-base

Minimal synthetic repro for the OpenTaint issue **"Vert.x verticle `start()`
defined in an ABSTRACT base class is not auto-discovered as an entry point"**
(see [`../../../issue.md`](../../../issue.md)).

This is a *local* benchmark project: it has no upstream git URL and is consumed
in-place by the regression harness via the `path:` field in
[`../../repos.yaml`](../../repos.yaml). Its rules and dataflow approximations
live under [`../../extensions/vertx-cors-abstract-base/`](../../extensions/vertx-cors-abstract-base/).

## What it exercises

Reflected-`Origin` → `Access-Control-Allow-Origin` CORS flows in six shapes
(`src/main/kotlin/test/{VertxCors,CorsVerticle}.kt`):

| Method | CORS handler location | Fires on the buggy engine? |
| --- | --- | --- |
| `VertxCors.plainCors` | plain method | ✅ |
| `VertxCors.directCors` | `route().handler { … }` | ✅ |
| `VertxCors.nestedCors` | `route().handler { addHeadersEndHandler { … } }` | ✅ |
| `ConcreteCorsVerticle.start` | `override suspend fun start()` in a **concrete** verticle | ✅ |
| `CorsVerticleBase.start` (via `MyVerticle`) | `override suspend fun start()` in an **abstract base** | ❌ — the bug |

The first four fire today; the abstract-base case does not, because entry-point
auto-discovery skips lifecycle/public methods inherited from an abstract base
class. When the engine is fixed, the fifth finding appears — the regression
harness then reports it as an *added* finding (verdict FAIL), flagging the
recovery. The project equally guards against a regression that drops any of the
four currently-firing cases.

## Building

A committed Gradle wrapper pins the build so the autobuilder compiles it
reproducibly on CI (it invokes the project's `gradlew`):

```
./gradlew clean classes
```

Vert.x / coroutines deps are `compileOnly` — the analyzer treats them as
external library methods and relies on the shipped dataflow approximations.
