package com.example.approximations.org_opensearch_client_opensearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;

@Approximate(org.opensearch.client.opensearch.core.bulk.BulkOperation.class)
public class BulkOperation {

    // BulkOperation.of(fn): the lambda fills a BulkOperation.Builder and
    // returns an ObjectBuilder<BulkOperation>; of() builds it. Taint
    // introduced inside the lambda body flows into the returned operation.
    public static org.opensearch.client.opensearch.core.bulk.BulkOperation of(
            @ArgumentTypeContext Function fn) throws Throwable {
        org.opensearch.client.opensearch.core.bulk.BulkOperation.Builder b =
                new org.opensearch.client.opensearch.core.bulk.BulkOperation.Builder();
        org.opensearch.client.util.ObjectBuilder ob =
                (org.opensearch.client.util.ObjectBuilder) fn.apply(b);
        if (ob == null || OpentaintNdUtil.nextBool()) {
            return b.build();
        }
        return (org.opensearch.client.opensearch.core.bulk.BulkOperation) ob.build();
    }
}
