package com.example.approximations.java_util;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Predicate;

@Approximate(java.util.Collection.class)
public class Collection {

    // Model: each tainted element flows into the predicate.
    public boolean removeIf(@ArgumentTypeContext Predicate filter) {
        java.util.Collection self = (java.util.Collection) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            java.util.Iterator it = self.iterator();
            while (it.hasNext()) {
                filter.test(it.next());
            }
        }
        return OpentaintNdUtil.nextBool();
    }
}
