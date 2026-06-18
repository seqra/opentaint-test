package com.example.approximations.java_util;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Approximate(java.util.ArrayList.class)
public class ArrayList {

    // Model: each tainted element flows into the consumer.
    public void forEach(@ArgumentTypeContext Consumer action) {
        java.util.ArrayList self = (java.util.ArrayList) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            for (Object e : self) {
                action.accept(e);
            }
        }
    }

    // Model: each tainted element flows into the predicate.
    public boolean removeIf(@ArgumentTypeContext Predicate filter) {
        java.util.ArrayList self = (java.util.ArrayList) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            for (Object e : self) {
                filter.test(e);
            }
        }
        return OpentaintNdUtil.nextBool();
    }
}
