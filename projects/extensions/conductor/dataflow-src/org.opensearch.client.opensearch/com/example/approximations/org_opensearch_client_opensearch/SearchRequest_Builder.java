package com.example.approximations.org_opensearch_client_opensearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;

@Approximate(org.opensearch.client.opensearch.core.SearchRequest.Builder.class)
public class SearchRequest_Builder {

    // SearchRequest.Builder.sort(fn): the lambda fills a SortOptions.Builder
    // and returns an ObjectBuilder<SortOptions>. Build it and feed it into
    // the concrete sort(SortOptions,...) setter so the tainted SortOptions
    // is stored on this builder (and hence in the built SearchRequest).
    public final org.opensearch.client.opensearch.core.SearchRequest.Builder sort(
            @ArgumentTypeContext Function fn) throws Throwable {
        org.opensearch.client.opensearch.core.SearchRequest.Builder self =
                (org.opensearch.client.opensearch.core.SearchRequest.Builder) (Object) this;
        org.opensearch.client.opensearch._types.SortOptions.Builder b =
                new org.opensearch.client.opensearch._types.SortOptions.Builder();
        org.opensearch.client.util.ObjectBuilder ob =
                (org.opensearch.client.util.ObjectBuilder) fn.apply(b);
        org.opensearch.client.opensearch._types.SortOptions opt;
        if (ob == null || OpentaintNdUtil.nextBool()) {
            opt = b.build();
        } else {
            opt = (org.opensearch.client.opensearch._types.SortOptions) ob.build();
        }
        return self.sort(opt);
    }

    // SearchRequest.Builder.source(fn): the lambda fills a SourceConfig.Builder
    // and returns an ObjectBuilder<SourceConfig>. Build it and feed it into the
    // concrete source(SourceConfig) setter so taint is stored on this builder.
    public final org.opensearch.client.opensearch.core.SearchRequest.Builder source(
            @ArgumentTypeContext Function fn) throws Throwable {
        org.opensearch.client.opensearch.core.SearchRequest.Builder self =
                (org.opensearch.client.opensearch.core.SearchRequest.Builder) (Object) this;
        org.opensearch.client.opensearch.core.search.SourceConfig.Builder b =
                new org.opensearch.client.opensearch.core.search.SourceConfig.Builder();
        org.opensearch.client.util.ObjectBuilder ob =
                (org.opensearch.client.util.ObjectBuilder) fn.apply(b);
        org.opensearch.client.opensearch.core.search.SourceConfig cfg;
        if (ob == null || OpentaintNdUtil.nextBool()) {
            cfg = b.build();
        } else {
            cfg = (org.opensearch.client.opensearch.core.search.SourceConfig) ob.build();
        }
        return self.source(cfg);
    }
}
