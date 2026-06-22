package com.example.approximations;

import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

@Approximate(kotlinx.coroutines.TimeoutKt.class)
public class TimeoutKt {
    public static Object withTimeout(long timeMillis, @ArgumentTypeContext Function2 block, Continuation continuation) {
        if (block == null) {
            return null;
        }
        return block.invoke(null, continuation);
    }
}
