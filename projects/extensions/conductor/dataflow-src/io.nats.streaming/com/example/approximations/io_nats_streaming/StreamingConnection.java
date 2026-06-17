package com.example.approximations.io_nats_streaming;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import io.nats.streaming.MessageHandler;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;

/**
 * Dataflow approximation for io.nats.streaming.StreamingConnection
 * (io.nats:java-nats-streaming:2.2.3).
 *
 * subscribe(...) registers a MessageHandler that the broker invokes
 * asynchronously, delivering each received Message into onMessage(Message).
 * This is a SOURCE-SIDE callback: the connection (this) is the broker-facing
 * endpoint, and the handler runs against broker-delivered data.
 *
 * Model: invoke the registered handler so the analyzer carries taint through
 * the callback boundary. A value captured in the handler's closure surfaces
 * when onMessage runs (the conductor *ObservableQueue receive idiom). The
 * Message argument itself is delivered null because io.nats.streaming.Message
 * has no constructor accessible outside its package, so a tainted Message
 * instance cannot be synthesised here (see tests_passing note).
 *
 * Lives in package com.example.approximations (NOT io.nats.streaming) so
 * @Approximate resolves the target class without colliding with it.
 */
@Approximate(io.nats.streaming.StreamingConnection.class)
public class StreamingConnection {

    // subscribe(String subject, MessageHandler cb) : Subscription
    public Subscription subscribe(String subject,
                                  @ArgumentTypeContext MessageHandler cb) {
        deliver(cb);
        return null;
    }

    // subscribe(String subject, MessageHandler cb, SubscriptionOptions opts) : Subscription
    public Subscription subscribe(String subject,
                                  @ArgumentTypeContext MessageHandler cb,
                                  SubscriptionOptions opts) {
        deliver(cb);
        return null;
    }

    // subscribe(String subject, String queue, MessageHandler cb) : Subscription
    public Subscription subscribe(String subject, String queue,
                                  @ArgumentTypeContext MessageHandler cb) {
        deliver(cb);
        return null;
    }

    // subscribe(String subject, String queue, MessageHandler cb,
    //           SubscriptionOptions opts) : Subscription
    public Subscription subscribe(String subject, String queue,
                                  @ArgumentTypeContext MessageHandler cb,
                                  SubscriptionOptions opts) {
        deliver(cb);
        return null;
    }

    // Drives the registered handler the way the broker would, so the analyzer
    // carries taint across the asynchronous callback boundary into onMessage.
    private void deliver(MessageHandler cb) {
        if (cb == null) {
            return;
        }
        if (OpentaintNdUtil.nextBool()) {
            cb.onMessage(null);
        }
    }
}
