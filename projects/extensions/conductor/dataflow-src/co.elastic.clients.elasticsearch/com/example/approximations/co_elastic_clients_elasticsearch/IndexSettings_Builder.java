package com.example.approximations.co_elastic_clients_elasticsearch;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for
 * co.elastic.clients.elasticsearch.indices.IndexSettings.Builder#lifecycle(Function).
 *
 * The fluent overload applies the function to a fresh IndexSettingsLifecycle
 * builder, builds the IndexSettingsLifecycle, stores it on this IndexSettings
 * builder via the typed lifecycle(...) setter, and returns this. Taint the
 * function writes into the lifecycle builder thus reaches the built
 * IndexSettings (recoverable via settings.lifecycle()).
 */
@Approximate(co.elastic.clients.elasticsearch.indices.IndexSettings.Builder.class)
public class IndexSettings_Builder {

    public co.elastic.clients.elasticsearch.indices.IndexSettings.Builder lifecycle(
            @ArgumentTypeContext Function fn) throws Throwable {
        co.elastic.clients.elasticsearch.indices.IndexSettings.Builder self =
                (co.elastic.clients.elasticsearch.indices.IndexSettings.Builder) (Object) this;
        co.elastic.clients.elasticsearch.indices.IndexSettingsLifecycle.Builder lb =
                new co.elastic.clients.elasticsearch.indices.IndexSettingsLifecycle.Builder();
        co.elastic.clients.util.ObjectBuilder ob =
                (co.elastic.clients.util.ObjectBuilder) fn.apply(lb);
        co.elastic.clients.elasticsearch.indices.IndexSettingsLifecycle lifecycle =
                (co.elastic.clients.elasticsearch.indices.IndexSettingsLifecycle) ob.build();
        return self.lifecycle(lifecycle);
    }
}
