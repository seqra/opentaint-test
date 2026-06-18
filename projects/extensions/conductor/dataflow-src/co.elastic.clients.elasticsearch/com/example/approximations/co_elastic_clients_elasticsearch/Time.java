package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for co.elastic.clients.elasticsearch._types.Time.
 *
 * Time.of(fn) applies the supplied function to a fresh Time.Builder and builds.
 * Taint the function writes into the builder (e.g. time(tainted)) reaches the
 * built Time.
 */
@Approximate(co.elastic.clients.elasticsearch._types.Time.class)
public class Time {

    public static co.elastic.clients.elasticsearch._types.Time of(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch._types.Time.Builder builder =
                new co.elastic.clients.elasticsearch._types.Time.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        return (co.elastic.clients.elasticsearch._types.Time) ob.build();
    }
}
