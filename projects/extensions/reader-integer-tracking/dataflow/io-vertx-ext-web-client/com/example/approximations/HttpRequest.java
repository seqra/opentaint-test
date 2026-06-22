package com.example.approximations;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.client.HttpResponse;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(io.vertx.ext.web.client.HttpRequest.class)
public class HttpRequest<T> {
    public void send(@ArgumentTypeContext Handler<AsyncResult<HttpResponse<T>>> handler) {
        io.vertx.ext.web.client.HttpRequest<T> self =
                (io.vertx.ext.web.client.HttpRequest<T>) (Object) this;
        if (handler != null && OpentaintNdUtil.nextBool()) {
            handler.handle((AsyncResult<HttpResponse<T>>) (Object) self);
        }
    }
}
