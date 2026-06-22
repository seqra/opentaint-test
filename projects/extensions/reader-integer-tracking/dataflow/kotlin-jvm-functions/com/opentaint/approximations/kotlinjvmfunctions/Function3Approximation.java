package com.opentaint.approximations.kotlinjvmfunctions;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(kotlin.jvm.functions.Function3.class)
public class Function3Approximation {
    public Object invoke(Object first, Object second, Object third) {
        if (OpentaintNdUtil.nextBool()) {
            return first;
        }
        if (OpentaintNdUtil.nextBool()) {
            return second;
        }
        return third;
    }
}
