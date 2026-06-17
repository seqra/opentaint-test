package com.example.approximations.java_util;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@Approximate(java.util.HashMap.class)
public class HashMap {

    // Model: the (tainted) key flows into the mapping function, whose result is
    // returned. We do NOT store the result back into the map, so taint stays on
    // the returned value and does not leak to reads under other keys.
    public Object computeIfAbsent(Object key, @ArgumentTypeContext Function mappingFunction) {
        if (OpentaintNdUtil.nextBool()) {
            return null;
        }
        return mappingFunction.apply(key);
    }

    // Model: the (tainted) new value flows into the remapping BiFunction; its
    // result is stored and returned.
    public Object merge(Object key, Object value, @ArgumentTypeContext BiFunction remappingFunction) {
        java.util.HashMap self = (java.util.HashMap) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            self.put(key, value);
            return value;
        }
        Object result = remappingFunction.apply(self.get(key), value);
        self.put(key, result);
        return result;
    }

    // Model: each stored key/value flows into the BiConsumer.
    public void forEach(@ArgumentTypeContext BiConsumer action) {
        java.util.HashMap self = (java.util.HashMap) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            for (Object key : self.keySet()) {
                action.accept(key, self.get(key));
            }
        }
    }

    // Model: each key/value flows into the BiFunction; its result is stored back.
    public void replaceAll(@ArgumentTypeContext BiFunction function) {
        java.util.HashMap self = (java.util.HashMap) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            for (Object key : self.keySet()) {
                Object newValue = function.apply(key, self.get(key));
                self.put(key, newValue);
            }
        }
    }
}
