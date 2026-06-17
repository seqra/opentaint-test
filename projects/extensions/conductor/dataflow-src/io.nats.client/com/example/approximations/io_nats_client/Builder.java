package com.example.approximations.io_nats_client;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import io.nats.client.ConnectionListener;

@Approximate(io.nats.client.Options.Builder.class)
public class Builder {

    // Model: connectionListener registers a ConnectionListener that the NATS
    // client later invokes with connection events (a source-side callback).
    // Invoking the listener surfaces any value its closure carries to the sink.
    // Returns the builder itself for fluent chaining (preserve container taint).
    public io.nats.client.Options.Builder connectionListener(
            @ArgumentTypeContext ConnectionListener listener) {
        io.nats.client.Options.Builder self = (io.nats.client.Options.Builder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            listener.connectionEvent(null, ConnectionListener.Events.CONNECTED);
        }
        return self;
    }
}
