package com.example.approximations.io_nats_client;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import io.nats.client.MessageHandler;

@Approximate(io.nats.client.Connection.class)
public class Connection {

    // Model: createDispatcher registers a MessageHandler that the NATS client
    // later invokes with inbound messages (a source-side callback). Invoking
    // the handler surfaces any value the handler closure carries to the sink,
    // and delivers a message the client received from the connection.
    public io.nats.client.Dispatcher createDispatcher(@ArgumentTypeContext MessageHandler handler)
            throws Throwable {
        io.nats.client.Connection self = (io.nats.client.Connection) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            handler.onMessage(null);
        }
        return self.createDispatcher();
    }
}
