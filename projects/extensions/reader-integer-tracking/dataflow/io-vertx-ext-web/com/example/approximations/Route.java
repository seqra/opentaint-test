package com.example.approximations;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(io.vertx.ext.web.Route.class)
public class Route {

    public io.vertx.ext.web.Route handler(@ArgumentTypeContext Handler<RoutingContext> requestHandler) {
        io.vertx.ext.web.Route self = (io.vertx.ext.web.Route) (Object) this;
        if (OpentaintNdUtil.nextBool()) return self;
        requestHandler.handle((RoutingContext) (Object) self);
        return self;
    }
}
