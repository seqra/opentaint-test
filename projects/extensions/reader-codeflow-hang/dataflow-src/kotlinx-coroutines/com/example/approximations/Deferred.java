package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;

/**
 * Models kotlinx.coroutines.Deferred#await.
 *
 * await() suspends and unwraps the value held by the Deferred. The value held by
 * the Deferred propagates to await()'s return value (receiver -> return). The
 * receiver carries the taint of the value it was created with (see the
 * CompletableDeferred factory companion approximation), so returning the receiver
 * propagates that taint to the unwrapped result.
 */
@Approximate(kotlinx.coroutines.Deferred.class)
public class Deferred {

    // signature: (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public Object await(kotlin.coroutines.Continuation continuation) {
        // receiver -> return: the held value flows out through await().
        return (Object) this;
    }
}
