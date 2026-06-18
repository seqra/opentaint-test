package com.example.approximations.org_apache_kafka;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;

@Approximate(org.apache.kafka.clients.producer.KafkaProducer.class)
public class KafkaProducer {

    // Model: send delivers the (tainted) record's payload to the completion
    // callback. The callback signature only exposes RecordMetadata, but a
    // closure capturing the record surfaces its taint when the callback runs,
    // so invoking the callback materializes that flow.
    public java.util.concurrent.Future send(ProducerRecord record, @ArgumentTypeContext Callback callback) {
        if (OpentaintNdUtil.nextBool()) {
            callback.onCompletion(null, null);
        }
        return null;
    }
}
