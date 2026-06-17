package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for
 * co.elastic.clients.elasticsearch.core.bulk.BulkOperation.
 *
 * BulkOperation.of(fn) applies the function to a fresh BulkOperation.Builder
 * and builds. Taint written into the builder reaches the built BulkOperation.
 */
@Approximate(co.elastic.clients.elasticsearch.core.bulk.BulkOperation.class)
public class BulkOperation {

    public static co.elastic.clients.elasticsearch.core.bulk.BulkOperation of(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch.core.bulk.BulkOperation.Builder builder =
                new co.elastic.clients.elasticsearch.core.bulk.BulkOperation.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(builder);
        return (co.elastic.clients.elasticsearch.core.bulk.BulkOperation) ob.build();
    }
}
