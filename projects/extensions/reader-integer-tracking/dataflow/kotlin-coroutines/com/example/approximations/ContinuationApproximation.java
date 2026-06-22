package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;

@Approximate(kotlin.coroutines.Continuation.class)
public class ContinuationApproximation {
    public void resumeWith(Object result) {
        StringBuilder self = (StringBuilder) (Object) this;
        self.append(result);

        StringBuilder resultContainer = (StringBuilder) result;
        resultContainer.append(this);
    }
}
