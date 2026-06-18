package com.example.approximations.org_opensearch_client_opensearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;

@Approximate(org.opensearch.client.opensearch._types.query_dsl.Query.class)
public class Query {

    // Query.of(fn): the lambda fills a Query.Builder and returns an
    // ObjectBuilder<Query>; of() builds it. Taint introduced inside the
    // lambda body flows into the returned Query.
    public static org.opensearch.client.opensearch._types.query_dsl.Query of(
            @ArgumentTypeContext Function fn) throws Throwable {
        org.opensearch.client.opensearch._types.query_dsl.Query.Builder b =
                new org.opensearch.client.opensearch._types.query_dsl.Query.Builder();
        org.opensearch.client.util.ObjectBuilder ob =
                (org.opensearch.client.util.ObjectBuilder) fn.apply(b);
        if (ob == null || OpentaintNdUtil.nextBool()) {
            return b.build();
        }
        return (org.opensearch.client.opensearch._types.query_dsl.Query) ob.build();
    }
}
