package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for co.elastic.clients.elasticsearch._types.SortOptions.
 *
 * SortOptions.of(fn) is a fluent builder factory: it hands a fresh Builder to
 * the supplied function, which configures it (closing over user data) and
 * returns an ObjectBuilder<SortOptions>; the factory then calls build(). Taint
 * the function writes into the builder ends up in the built SortOptions, so the
 * model just applies the function and builds.
 */
@Approximate(co.elastic.clients.elasticsearch._types.SortOptions.class)
public class SortOptions {

    public static co.elastic.clients.elasticsearch._types.SortOptions of(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch._types.SortOptions.Builder builder =
                new co.elastic.clients.elasticsearch._types.SortOptions.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        return (co.elastic.clients.elasticsearch._types.SortOptions) ob.build();
    }
}
