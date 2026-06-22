package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;

/**
 * Companion stub for kotlinx.coroutines.CompletableDeferredKt#CompletableDeferred(T).
 *
 * The factory CompletableDeferred(value) wraps `value` inside a CompletableDeferred.
 * The held value's taint must reach the Deferred object so that the Deferred#await
 * receiver->return model can unwrap it again. This models arg(0) -> return: the
 * returned Deferred carries the taint of the value it was created with.
 */
@Approximate(kotlinx.coroutines.CompletableDeferredKt.class)
public class CompletableDeferredKt {

    // signature: (Ljava/lang/Object;)Lkotlinx/coroutines/CompletableDeferred;
    public static kotlinx.coroutines.CompletableDeferred CompletableDeferred(Object value) {
        // arg(0) -> return: the value flows into the returned Deferred container.
        return (kotlinx.coroutines.CompletableDeferred) value;
    }
}
