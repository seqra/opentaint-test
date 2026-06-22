package com.opentaint.approximations.kotlinjvmfunctions;

import org.opentaint.ir.approximation.annotation.Approximate;

@Approximate(kotlin.jvm.functions.Function1.class)
public class Function1Approximation {
    public Object invoke(Object value) {
        return value;
    }
}
