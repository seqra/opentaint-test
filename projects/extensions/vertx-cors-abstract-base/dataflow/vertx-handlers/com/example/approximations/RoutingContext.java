package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for io.vertx.ext.web.RoutingContext end-handler registration.
 *
 * RoutingContext#addHeadersEndHandler / #addBodyEndHandler take a Handler<Void> and are dropped as
 * external methods; the engine never invokes the handler, so a nested
 * `rc.addHeadersEndHandler { v -> ... }` body (the real CORS / EPUB shape) is unreachable. Modeling
 * them as INVOKING handler.handle(...) makes the nested lambda body reachable.
 *
 * In vertx-web 3.8.1 these return int (the handler id), not RoutingContext, so the modeled methods
 * return int.
 */
@Approximate(io.vertx.ext.web.RoutingContext.class)
public class RoutingContext {

    public int addHeadersEndHandler(@ArgumentTypeContext io.vertx.core.Handler handler) {
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return 0;
    }

    public int addBodyEndHandler(@ArgumentTypeContext io.vertx.core.Handler handler) {
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return 0;
    }
}
