package com.example.approximations;

import kotlin.coroutines.Continuation;
import org.opentaint.ir.approximation.annotation.Approximate;

@Approximate(kotlinx.coroutines.Deferred.class)
public class Deferred {
    public Object await(Continuation continuation) {
        kotlinx.coroutines.Deferred self = (kotlinx.coroutines.Deferred) (Object) this;
        return self;
    }
}
