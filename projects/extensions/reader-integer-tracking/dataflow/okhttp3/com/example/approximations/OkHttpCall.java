package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(okhttp3.Call.class)
public class OkHttpCall {
    public void enqueue(@ArgumentTypeContext okhttp3.Callback responseCallback) {
        okhttp3.Call self = (okhttp3.Call) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            responseCallback.onFailure(self, null);
        }
        if (OpentaintNdUtil.nextBool()) {
            try {
                responseCallback.onResponse(self, null);
            } catch (java.io.IOException ignored) {
                // The approximation only needs callback reachability for taint propagation.
            }
        }
    }
}
