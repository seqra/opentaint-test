package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(okhttp3.OkHttpClient.Builder.class)
public class OkHttpClientBuilder {
    public okhttp3.OkHttpClient.Builder addInterceptor(
            @ArgumentTypeContext okhttp3.Interceptor interceptor) {
        okhttp3.OkHttpClient.Builder self = (okhttp3.OkHttpClient.Builder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            try {
                interceptor.intercept(null);
            } catch (java.io.IOException ignored) {
                // The approximation only needs callback reachability for taint propagation.
            }
        }
        return self;
    }

    public okhttp3.OkHttpClient.Builder addNetworkInterceptor(
            @ArgumentTypeContext okhttp3.Interceptor interceptor) {
        okhttp3.OkHttpClient.Builder self = (okhttp3.OkHttpClient.Builder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            try {
                interceptor.intercept(null);
            } catch (java.io.IOException ignored) {
                // The approximation only needs callback reachability for taint propagation.
            }
        }
        return self;
    }

    public okhttp3.OkHttpClient.Builder proxyAuthenticator(
            @ArgumentTypeContext okhttp3.Authenticator proxyAuthenticator) {
        okhttp3.OkHttpClient.Builder self = (okhttp3.OkHttpClient.Builder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            try {
                proxyAuthenticator.authenticate(null, null);
            } catch (java.io.IOException ignored) {
                // The approximation only needs callback reachability for taint propagation.
            }
        }
        return self;
    }
}
