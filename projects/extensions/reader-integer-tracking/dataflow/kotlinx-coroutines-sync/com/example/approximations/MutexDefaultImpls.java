package com.example.approximations;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(kotlinx.coroutines.sync.Mutex.DefaultImpls.class)
public class MutexDefaultImpls {
    public static Object lock$default(
            kotlinx.coroutines.sync.Mutex mutex,
            Object owner,
            Continuation continuation,
            int mask,
            Object defaultMarker) {
        if (continuation != null && OpentaintNdUtil.nextBool()) {
            continuation.resumeWith(Unit.INSTANCE);
        }
        if (OpentaintNdUtil.nextBool()) {
            return Unit.INSTANCE;
        }
        return IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }
}
