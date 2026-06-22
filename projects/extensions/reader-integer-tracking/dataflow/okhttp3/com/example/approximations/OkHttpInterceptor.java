package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;

@Approximate(okhttp3.Interceptor.class)
public class OkHttpInterceptor {
    public okhttp3.Response intercept(okhttp3.Interceptor.Chain chain) throws java.io.IOException {
        okhttp3.Interceptor self = (okhttp3.Interceptor) (Object) this;
        return self.intercept(chain);
    }
}
