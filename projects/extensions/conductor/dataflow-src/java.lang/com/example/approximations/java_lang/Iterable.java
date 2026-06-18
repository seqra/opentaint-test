package com.example.approximations.java_lang;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Consumer;

@Approximate(java.lang.Iterable.class)
public class Iterable {

    // Model: each tainted element flows into the consumer.
    public void forEach(@ArgumentTypeContext Consumer action) {
        java.lang.Iterable self = (java.lang.Iterable) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            java.util.Iterator it = self.iterator();
            while (it.hasNext()) {
                action.accept(it.next());
            }
        }
    }
}
