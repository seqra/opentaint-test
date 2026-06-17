package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for
 * co.elastic.clients.elasticsearch.ilm.ElasticsearchIlmClient#getLifecycle.
 *
 * getLifecycle(fn) hands a fresh GetLifecycleRequest.Builder to the function,
 * builds the request and sends it. We build the request, read its tainted
 * name(), and surface it through the response: GetLifecycleResponse exposes a
 * Map<String,Lifecycle> result(); we seed that map under the tainted name key
 * so response.result() carries the taint.
 */
@Approximate(co.elastic.clients.elasticsearch.ilm.ElasticsearchIlmClient.class)
public class ElasticsearchIlmClient {

    public co.elastic.clients.elasticsearch.ilm.GetLifecycleResponse getLifecycle(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch.ilm.GetLifecycleRequest.Builder builder =
                new co.elastic.clients.elasticsearch.ilm.GetLifecycleRequest.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        final co.elastic.clients.elasticsearch.ilm.GetLifecycleRequest request =
                (co.elastic.clients.elasticsearch.ilm.GetLifecycleRequest) ob.build();
        final String name = request.name();
        return co.elastic.clients.elasticsearch.ilm.GetLifecycleResponse.of(
                b -> b.result(name, l -> l));
    }
}
