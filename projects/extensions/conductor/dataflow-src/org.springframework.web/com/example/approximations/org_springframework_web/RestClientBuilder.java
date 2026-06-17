package com.example.approximations.org_springframework_web;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Consumer;

/**
 * Dataflow approximation for org.springframework.web.client.RestClient$Builder.
 *
 * defaultHeaders(Consumer<HttpHeaders>) hands the builder's HttpHeaders to the
 * supplied consumer so the caller can populate the default request headers. The
 * consumer is the unit of taint: whatever the lambda body writes (often a
 * tainted header value) is the propagated effect. Without invoking the consumer
 * the analyzer cannot see that flow, so the approximation runs it against a
 * fresh HttpHeaders (callback boundary), letting any taint the lambda carries —
 * into the headers argument or into state it captures — be analyzed. The method
 * returns the builder for chaining, so return `self`.
 */
@Approximate(org.springframework.web.client.RestClient.Builder.class)
public class RestClientBuilder {

    public org.springframework.web.client.RestClient.Builder defaultHeaders(
            @ArgumentTypeContext Consumer headersConsumer) {
        org.springframework.web.client.RestClient.Builder self =
                (org.springframework.web.client.RestClient.Builder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            headersConsumer.accept(new org.springframework.http.HttpHeaders());
        }
        return self;
    }
}
