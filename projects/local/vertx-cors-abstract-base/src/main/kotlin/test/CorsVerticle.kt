package test

import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle

abstract class CorsVerticleBase : CoroutineVerticle() {
    lateinit var router: Router
    // C4 — CORS handler inside the verticle's suspend start() (the EXACT RestVerticle.start shape)
    override suspend fun start() {
        router.route().handler { ctx ->
            ctx.addHeadersEndHandler {
                val origin = ctx.request().getHeader("Origin")
                ctx.response().putHeader("Access-Control-Allow-Origin", origin)
            }
        }
        initRouter(router)
    }
    abstract suspend fun initRouter(router: Router)
}

class MyVerticle : CorsVerticleBase() {
    override suspend fun initRouter(router: Router) {
        // C5 — a source->sink INSIDE initRouter (which is reached as a root, like YueduApi.initRouter)
        router.route().handler { ctx ->
            val p = ctx.request().getParam("x")
            ctx.response().sendFile(p)   // path-traversal sink to confirm initRouter IS analyzed
        }
    }
}

// C6 — CORS in start() of a CONCRETE verticle (start defined directly here, not inherited from an abstract base)
class ConcreteCorsVerticle : CoroutineVerticle() {
    lateinit var router: Router
    override suspend fun start() {
        router.route().handler { ctx ->
            ctx.addHeadersEndHandler {
                val origin = ctx.request().getHeader("Origin")
                ctx.response().putHeader("Access-Control-Allow-Origin", origin)
            }
        }
    }
}
