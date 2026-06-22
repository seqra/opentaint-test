package com.example.approximations;

import io.vertx.core.Handler;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(io.vertx.ext.web.RoutingContext.class)
public class RoutingContext {

    public int addHeadersEndHandler(@ArgumentTypeContext Handler<Void> handler) {
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return 0;
    }
}
