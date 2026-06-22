package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(okhttp3.logging.HttpLoggingInterceptor.class)
public class HttpLoggingInterceptor {
    public HttpLoggingInterceptor() {
    }

    public HttpLoggingInterceptor(okhttp3.logging.HttpLoggingInterceptor.Logger logger) {
        storeLogger(logger);
    }

    public HttpLoggingInterceptor(
            okhttp3.logging.HttpLoggingInterceptor.Logger logger,
            int mask,
            kotlin.jvm.internal.DefaultConstructorMarker marker) {
        storeLogger(logger);
    }

    private void storeLogger(okhttp3.logging.HttpLoggingInterceptor.Logger logger) {
        if (logger == null || OpentaintNdUtil.nextBool()) return;
        StringBuilder self = (StringBuilder) (Object) this;
        self.append((Object) logger);
    }
}
