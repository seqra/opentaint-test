package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for co.elastic.clients.elasticsearch.ilm.IlmPolicy.
 *
 * IlmPolicy.of(fn) applies the function to a fresh IlmPolicy.Builder and
 * builds. Taint written into the builder reaches the built IlmPolicy.
 */
@Approximate(co.elastic.clients.elasticsearch.ilm.IlmPolicy.class)
public class IlmPolicy {

    public static co.elastic.clients.elasticsearch.ilm.IlmPolicy of(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch.ilm.IlmPolicy.Builder builder =
                new co.elastic.clients.elasticsearch.ilm.IlmPolicy.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        return (co.elastic.clients.elasticsearch.ilm.IlmPolicy) ob.build();
    }
}
