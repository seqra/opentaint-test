package com.opentaint.approximations.kotlinjvmfunctions;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(kotlin.jvm.functions.Function2.class)
public class Function2Approximation {
    public Object invoke(Object first, Object second) {
        if (OpentaintNdUtil.nextBool()) {
            return first;
        }
        return second;
    }
}
