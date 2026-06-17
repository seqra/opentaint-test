package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for
 * co.elastic.clients.elasticsearch._types.query_dsl.Query.
 *
 * Query.of(fn) applies the function to a fresh Query.Builder and builds. Taint
 * written into the builder (e.g. a tainted query string) reaches the built
 * Query.
 */
@Approximate(co.elastic.clients.elasticsearch._types.query_dsl.Query.class)
public class Query {

    public static co.elastic.clients.elasticsearch._types.query_dsl.Query of(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch._types.query_dsl.Query.Builder builder =
                new co.elastic.clients.elasticsearch._types.query_dsl.Query.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        return (co.elastic.clients.elasticsearch._types.query_dsl.Query) ob.build();
    }
}
