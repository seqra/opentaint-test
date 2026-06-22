package com.example.approximations;

import kotlin.coroutines.Continuation;
import org.opentaint.ir.approximation.annotation.Approximate;

@Approximate(kotlinx.coroutines.DelayKt.class)
public class DelayKt {
    public static Object delay(long timeMillis, Continuation continuation) {
        return continuation;
    }
}
