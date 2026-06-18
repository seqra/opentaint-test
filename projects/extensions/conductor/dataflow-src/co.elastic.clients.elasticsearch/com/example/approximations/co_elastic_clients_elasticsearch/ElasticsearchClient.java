package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for co.elastic.clients.elasticsearch.ElasticsearchClient
 * fluent request methods (count / get / search).
 *
 * Each method hands a fresh request Builder to the supplied function, which
 * configures it (closing over user-controlled data) and returns an
 * ObjectBuilder<XxxRequest>; the client builds the request and sends it. The
 * tainted request reaching Elasticsearch is the propagation of interest. We
 * model it by building the request, pulling a tainted String field back out,
 * and seeding the returned response with it so a downstream getter on the
 * response observes the taint (e.g. response.id(), response.scrollId()).
 */
@Approximate(co.elastic.clients.elasticsearch.ElasticsearchClient.class)
public class ElasticsearchClient {

    // count(Function): build the CountRequest from the lambda. CountResponse
    // has no String field to carry the tainted request value back out, so the
    // built request is the propagation endpoint (the request reaching ES).
    public co.elastic.clients.elasticsearch.core.CountResponse count(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch.core.CountRequest.Builder builder =
                new co.elastic.clients.elasticsearch.core.CountRequest.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        final co.elastic.clients.elasticsearch.core.CountRequest request =
                (co.elastic.clients.elasticsearch.core.CountRequest) ob.build();
        return co.elastic.clients.elasticsearch.core.CountResponse.of(
                b -> b.count(request.q() == null ? 0L : (long) request.q().length()));
    }

    public co.elastic.clients.elasticsearch.core.GetResponse get(
            @ArgumentTypeContext Function fn, Class cls) throws Throwable {
        co.elastic.clients.elasticsearch.core.GetRequest.Builder builder =
                new co.elastic.clients.elasticsearch.core.GetRequest.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        final co.elastic.clients.elasticsearch.core.GetRequest request =
                (co.elastic.clients.elasticsearch.core.GetRequest) ob.build();
        return co.elastic.clients.elasticsearch.core.GetResponse.of(
                b -> b.id(request.id()).index(request.index()).found(true));
    }

    public co.elastic.clients.elasticsearch.core.SearchResponse search(
            @ArgumentTypeContext Function fn, Class cls) throws Throwable {
        co.elastic.clients.elasticsearch.core.SearchRequest.Builder builder =
                new co.elastic.clients.elasticsearch.core.SearchRequest.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        final co.elastic.clients.elasticsearch.core.SearchRequest request =
                (co.elastic.clients.elasticsearch.core.SearchRequest) ob.build();
        return co.elastic.clients.elasticsearch.core.SearchResponse.of(
                b -> b.scrollId(request.q()));
    }
}
