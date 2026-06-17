package com.example.approximations.java_lang;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Supplier;

@Approximate(java.lang.ThreadLocal.class)
public class ThreadLocal {

    // Model: the supplier result becomes the ThreadLocal's value, retrievable via get().
    public static java.lang.ThreadLocal withInitial(@ArgumentTypeContext Supplier supplier) {
        java.lang.ThreadLocal result = new java.lang.ThreadLocal();
        if (OpentaintNdUtil.nextBool()) {
            result.set(supplier.get());
        }
        return result;
    }
}
