package com.example.approximations;

import io.vertx.core.Handler;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(io.vertx.core.http.HttpConnection.class)
public class HttpConnection {
    public io.vertx.core.http.HttpConnection closeHandler(@ArgumentTypeContext Handler<Void> handler) {
        io.vertx.core.http.HttpConnection self = (io.vertx.core.http.HttpConnection) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            handler.handle(null);
        }
        return self;
    }
}
