package com.example.approximations.org_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for org.elasticsearch.client.RestClientBuilder.
 *
 * setHttpClientConfigCallback(HttpClientConfigCallback) and
 * setRequestConfigCallback(RequestConfigCallback) register a customization
 * callback on the builder. The callback is a single-abstract-method functional
 * interface: it receives a fresh Apache HttpComponents builder
 * (HttpAsyncClientBuilder / RequestConfig.Builder), may mutate it with caller
 * data (credentials, hosts, timeouts), and returns it. The lambda body is the
 * unit of taint; without invoking the callback the analyzer cannot see what it
 * writes into the builder or into the state it captures. The approximation runs
 * the callback against a fresh builder (the callback boundary) so any taint the
 * lambda carries is analyzed. Both setters return the builder for chaining, so
 * return `self`.
 */
@Approximate(org.elasticsearch.client.RestClientBuilder.class)
public class RestClientBuilder {

    public org.elasticsearch.client.RestClientBuilder setHttpClientConfigCallback(
            @ArgumentTypeContext org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback callback) {
        org.elasticsearch.client.RestClientBuilder self =
                (org.elasticsearch.client.RestClientBuilder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            callback.customizeHttpClient(
                    org.apache.http.impl.nio.client.HttpAsyncClientBuilder.create());
        }
        return self;
    }

    public org.elasticsearch.client.RestClientBuilder setRequestConfigCallback(
            @ArgumentTypeContext org.elasticsearch.client.RestClientBuilder.RequestConfigCallback callback) {
        org.elasticsearch.client.RestClientBuilder self =
                (org.elasticsearch.client.RestClientBuilder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            callback.customizeRequestConfig(
                    org.apache.http.client.config.RequestConfig.custom());
        }
        return self;
    }
}
