package test

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object VertxCors {
    // C1 — EXACT codex RestVerticle pattern: nested addHeadersEndHandler capturing the OUTER ctx
    fun nestedCors(router: Router) {
        router.route().handler { ctx ->
            ctx.addHeadersEndHandler {
                val origin = ctx.request().getHeader("Origin")
                ctx.response().putHeader("Access-Control-Allow-Origin", origin)
            }
        }
    }

    // C2 — single-level: getHeader -> putHeader directly in the route handler (no nesting)
    fun directCors(router: Router) {
        router.route().handler { ctx ->
            val origin = ctx.request().getHeader("Origin")
            ctx.response().putHeader("Access-Control-Allow-Origin", origin)
        }
    }

    // C3 — plain method (no handler lambda at all) — control
    fun plainCors(ctx: RoutingContext) {
        val origin = ctx.request().getHeader("Origin")
        ctx.response().putHeader("Access-Control-Allow-Origin", origin)
    }
}
