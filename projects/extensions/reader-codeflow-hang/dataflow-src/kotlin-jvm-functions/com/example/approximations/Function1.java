package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;

/**
 * Models kotlin.jvm.functions.Function1#invoke.
 *
 * The receiver is the lambda/callback itself. A single-arg Kotlin lambda body may
 * route its parameter into its return value, so taint on the argument may flow out
 * through the result. Model this conservatively: the result is the argument.
 */
@Approximate(kotlin.jvm.functions.Function1.class)
public class Function1 {

    public Object invoke(Object p1) {
        return p1;
    }
}
