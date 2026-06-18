package com.example.approximations.java_util_concurrent;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.concurrent.TimeUnit;

@Approximate(java.util.concurrent.ScheduledThreadPoolExecutor.class)
public class ScheduledThreadPoolExecutor {

    // Model: the submitted Runnable captures tainted state; running it lets that
    // taint surface at whatever the lambda body touches (callback boundary).
    public java.util.concurrent.ScheduledFuture schedule(@ArgumentTypeContext Runnable command, long delay, TimeUnit unit) {
        if (OpentaintNdUtil.nextBool()) {
            command.run();
        }
        return null;
    }
}
