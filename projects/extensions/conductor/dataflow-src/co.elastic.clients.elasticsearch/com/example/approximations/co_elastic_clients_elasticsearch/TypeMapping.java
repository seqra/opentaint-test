package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for
 * co.elastic.clients.elasticsearch._types.mapping.TypeMapping.
 *
 * TypeMapping.of(fn) applies the function to a fresh TypeMapping.Builder and
 * builds. Taint written into the builder reaches the built TypeMapping.
 */
@Approximate(co.elastic.clients.elasticsearch._types.mapping.TypeMapping.class)
public class TypeMapping {

    public static co.elastic.clients.elasticsearch._types.mapping.TypeMapping of(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch._types.mapping.TypeMapping.Builder builder =
                new co.elastic.clients.elasticsearch._types.mapping.TypeMapping.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        return (co.elastic.clients.elasticsearch._types.mapping.TypeMapping) ob.build();
    }
}
