package com.example.approximations.java_util;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@Approximate(java.util.Map.class)
public class Map {

    // Model: the (tainted) key flows into the mapping function, whose result is
    // returned. We do NOT store the result back into the map, so taint stays on
    // the returned value and does not leak to reads under other keys.
    public Object computeIfAbsent(Object key, @ArgumentTypeContext Function mappingFunction) {
        if (OpentaintNdUtil.nextBool()) {
            return null;
        }
        return mappingFunction.apply(key);
    }

    // Model: each stored key/value flows into the BiConsumer.
    public void forEach(@ArgumentTypeContext BiConsumer action) {
        java.util.Map self = (java.util.Map) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            for (Object key : self.keySet()) {
                action.accept(key, self.get(key));
            }
        }
    }

    // Model: each key/value flows into the BiFunction; its result is stored back
    // under the same key.
    public void replaceAll(@ArgumentTypeContext BiFunction function) {
        java.util.Map self = (java.util.Map) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            for (Object key : self.keySet()) {
                Object newValue = function.apply(key, self.get(key));
                self.put(key, newValue);
            }
        }
    }
}
