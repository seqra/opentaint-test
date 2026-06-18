package com.example.approximations.java_util_concurrent;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;

@Approximate(java.util.concurrent.ConcurrentHashMap.class)
public class ConcurrentHashMap {

    // Model: the (tainted) key flows into the mapping function, whose result is
    // returned. We do NOT store the result back into the map, so taint stays on
    // the returned value and does not leak to reads under other keys.
    public Object computeIfAbsent(Object key, @ArgumentTypeContext Function mappingFunction) {
        if (OpentaintNdUtil.nextBool()) {
            return null;
        }
        return mappingFunction.apply(key);
    }
}
