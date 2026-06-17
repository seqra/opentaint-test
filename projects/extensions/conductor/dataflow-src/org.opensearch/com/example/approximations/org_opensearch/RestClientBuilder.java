package com.example.approximations.org_opensearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for org.opensearch.client.RestClientBuilder.
 *
 * setHttpClientConfigCallback(HttpClientConfigCallback) and
 * setRequestConfigCallback(RequestConfigCallback) each store a single-method
 * callback that OpenSearch later invokes with an Apache HttpComponents builder
 * so the caller can customize the client / request config. The callback is the
 * unit of taint (dropped factPosition arg(0)): whatever the lambda body writes
 * into the builder it receives — or into state it captures — is the propagated
 * effect. Without invoking the callback the analyzer cannot see that flow, so
 * the approximation runs each callback against a fresh builder (callback
 * boundary). Both setters return the builder for chaining, so return `self`.
 */
@Approximate(org.opensearch.client.RestClientBuilder.class)
public class RestClientBuilder {

    public org.opensearch.client.RestClientBuilder setHttpClientConfigCallback(
            @ArgumentTypeContext org.opensearch.client.RestClientBuilder.HttpClientConfigCallback callback) {
        org.opensearch.client.RestClientBuilder self =
                (org.opensearch.client.RestClientBuilder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            callback.customizeHttpClient(
                    org.apache.http.impl.nio.client.HttpAsyncClientBuilder.create());
        }
        return self;
    }

    public org.opensearch.client.RestClientBuilder setRequestConfigCallback(
            @ArgumentTypeContext org.opensearch.client.RestClientBuilder.RequestConfigCallback callback) {
        org.opensearch.client.RestClientBuilder self =
                (org.opensearch.client.RestClientBuilder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            callback.customizeRequestConfig(
                    org.apache.http.client.config.RequestConfig.custom());
        }
        return self;
    }
}
