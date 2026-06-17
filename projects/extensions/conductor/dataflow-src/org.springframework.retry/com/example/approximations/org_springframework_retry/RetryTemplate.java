package com.example.approximations.org_springframework_retry;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import org.springframework.retry.RetryCallback;

/**
 * Dataflow approximation for org.springframework.retry.support.RetryTemplate.
 *
 * execute(RetryCallback) invokes the callback's doWithRetry(RetryContext) and
 * returns its result. Taint carried by the callback (its captured closure
 * state) reaches the returned value through doWithRetry's return. Model that
 * directly: invoke the callback and return what it produces.
 */
@Approximate(org.springframework.retry.support.RetryTemplate.class)
public class RetryTemplate {

    // execute(RetryCallback): callback result -> returned value.
    public Object execute(@ArgumentTypeContext RetryCallback callback) throws Throwable {
        return callback.doWithRetry(null);
    }
}
