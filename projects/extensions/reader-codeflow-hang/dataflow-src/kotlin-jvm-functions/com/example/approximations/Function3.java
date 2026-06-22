package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Models kotlin.jvm.functions.Function3#invoke.
 *
 * The receiver is the lambda/callback itself. A Kotlin lambda body may route any
 * of its parameters into its return value, so taint on any argument may flow out
 * through the result. Model this conservatively: on a non-deterministic path the
 * result is each argument in turn.
 */
@Approximate(kotlin.jvm.functions.Function3.class)
public class Function3 {

    public Object invoke(Object p1, Object p2, Object p3) {
        if (OpentaintNdUtil.nextBool()) {
            return p1;
        }
        if (OpentaintNdUtil.nextBool()) {
            return p2;
        }
        return p3;
    }
}
