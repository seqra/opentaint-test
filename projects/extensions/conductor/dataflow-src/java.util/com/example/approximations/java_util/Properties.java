package com.example.approximations.java_util;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.BiConsumer;

@Approximate(java.util.Properties.class)
public class Properties {

    // Model: each stored key/value flows into the BiConsumer.
    public void forEach(@ArgumentTypeContext BiConsumer action) {
        java.util.Properties self = (java.util.Properties) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            for (Object key : self.keySet()) {
                action.accept(key, self.get(key));
            }
        }
    }
}
