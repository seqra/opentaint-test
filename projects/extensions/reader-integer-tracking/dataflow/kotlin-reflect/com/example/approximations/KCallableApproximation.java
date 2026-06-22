package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(kotlin.reflect.KCallable.class)
public class KCallableApproximation {
    public Object call(Object... args) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (OpentaintNdUtil.nextBool()) {
                return arg;
            }
        }
        return null;
    }
}
