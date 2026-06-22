package com.example.approximations;

import kotlinx.coroutines.Job;
import org.opentaint.ir.approximation.annotation.Approximate;

@Approximate(kotlinx.coroutines.CompletableDeferredKt.class)
public class CompletableDeferredKt {
    public static kotlinx.coroutines.CompletableDeferred CompletableDeferred(Job parent) {
        return (kotlinx.coroutines.CompletableDeferred) (Object) parent;
    }

    public static <T> kotlinx.coroutines.CompletableDeferred<T> CompletableDeferred(T value) {
        return (kotlinx.coroutines.CompletableDeferred<T>) (Object) value;
    }
}
