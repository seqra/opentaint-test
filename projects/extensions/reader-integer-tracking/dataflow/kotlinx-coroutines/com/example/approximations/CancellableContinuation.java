package com.example.approximations;

import kotlin.jvm.functions.Function1;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

@Approximate(kotlinx.coroutines.CancellableContinuation.class)
public class CancellableContinuation {
    public void invokeOnCancellation(@ArgumentTypeContext Function1 handler) {
        handler.invoke(null);
    }
}
