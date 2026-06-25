package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for io.vertx.ext.web.Route higher-order handler registration.
 *
 * The engine drops Route#handler / #blockingHandler / #failureHandler as external methods and
 * never invokes the Handler<RoutingContext> lambda passed to them, so any source->sink flow living
 * inside an inline `route.handler { rc -> ... }` body is unreachable. Modeling these methods as
 * INVOKING handler.handle(...) makes the lambda body reachable (cf. the skill's subscribe(Consumer)
 * example: consumer.accept(...)).
 *
 * The event value passed to handle(...) does not need to carry taint here: the test/real flows call
 * the source fresh inside the lambda, so only reachability matters. We pass null under a
 * nondeterministic branch.
 */
@Approximate(io.vertx.ext.web.Route.class)
public class Route {

    public io.vertx.ext.web.Route handler(@ArgumentTypeContext io.vertx.core.Handler handler) {
        io.vertx.ext.web.Route self = (io.vertx.ext.web.Route) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return self;
    }

    public io.vertx.ext.web.Route blockingHandler(@ArgumentTypeContext io.vertx.core.Handler handler) {
        io.vertx.ext.web.Route self = (io.vertx.ext.web.Route) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return self;
    }

    public io.vertx.ext.web.Route blockingHandler(@ArgumentTypeContext io.vertx.core.Handler handler, boolean ordered) {
        io.vertx.ext.web.Route self = (io.vertx.ext.web.Route) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return self;
    }

    public io.vertx.ext.web.Route failureHandler(@ArgumentTypeContext io.vertx.core.Handler handler) {
        io.vertx.ext.web.Route self = (io.vertx.ext.web.Route) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return self;
    }
}
