package com.example.approximations.java_util;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Consumer;

@Approximate(java.util.Iterator.class)
public class Iterator {

    // Model: each remaining tainted element flows into the consumer.
    public void forEachRemaining(@ArgumentTypeContext Consumer action) {
        java.util.Iterator self = (java.util.Iterator) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            while (self.hasNext()) {
                action.accept(self.next());
            }
        }
    }
}
