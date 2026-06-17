package com.example.approximations.com_rabbitmq_client;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import com.rabbitmq.client.ShutdownListener;

@Approximate(com.rabbitmq.client.ShutdownNotifier.class)
public class ShutdownNotifier {

    // Model: when shutdown occurs the registered listener is invoked with this
    // notifier's close reason (a ShutdownSignalException). Taint carried by the
    // notifier's state therefore flows into the listener callback argument.
    public void addShutdownListener(@ArgumentTypeContext ShutdownListener listener) {
        com.rabbitmq.client.ShutdownNotifier self =
            (com.rabbitmq.client.ShutdownNotifier) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            listener.shutdownCompleted(self.getCloseReason());
        }
    }
}
