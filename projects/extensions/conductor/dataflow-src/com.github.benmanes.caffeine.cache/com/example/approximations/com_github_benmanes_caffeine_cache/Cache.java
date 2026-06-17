package com.example.approximations.com_github_benmanes_caffeine_cache;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;

/**
 * Model for Cache.get(key, mappingFunction): on a cache miss the mappingFunction is
 * invoked with the key, its result stored under the key and returned. We route a
 * tainted key through the function into the returned value. We do NOT store the
 * result back, so taint stays on the returned value and the key it was loaded for.
 */
@Approximate(com.github.benmanes.caffeine.cache.Cache.class)
public class Cache {

    // get(K, Function<? super K, ? extends V>): V
    public Object get(Object key, @ArgumentTypeContext Function mappingFunction) {
        if (OpentaintNdUtil.nextBool()) {
            return null;
        }
        return mappingFunction.apply(key);
    }
}
