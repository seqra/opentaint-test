package com.example.approximations.org_apache_kafka;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import org.apache.kafka.common.KafkaFuture;

@Approximate(org.apache.kafka.common.KafkaFuture.class)
public class KafkaFutureApprox {

    // Model: when the future completes, its (tainted) result is delivered as the
    // first argument of the BiConsumer. The future is returned unchanged so the
    // result stays extractable downstream (get/getNow). Returning self on the
    // skip path preserves the container's taint.
    public KafkaFuture whenComplete(@ArgumentTypeContext KafkaFuture.BiConsumer action) throws Throwable {
        KafkaFuture self = (KafkaFuture) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            return self;
        }
        // Taint carried by the future surfaces as the consumer's result argument.
        action.accept(self, null);
        return self;
    }
}
