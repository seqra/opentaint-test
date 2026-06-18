package com.example.approximations.com_github_benmanes_caffeine_cache;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for LoadingCache.get(key).
 *
 * A LoadingCache is built with a CacheLoader (Caffeine.build(loader)); on a miss
 * get(key) invokes loader.load(key) and caches/returns the result. The loader is
 * supplied at build time, far from this call, so we cannot reach it here. What
 * matters for taint is that the loaded value is derived from the key
 * (e.g. JsonQuery::compile(queryExpression) yields a query embedding the key), so
 * a tainted key produces a tainted loaded value. We model that key -> result
 * propagation directly. We do NOT store it back under a key, so taint does not
 * leak to reads under other keys.
 */
@Approximate(com.github.benmanes.caffeine.cache.LoadingCache.class)
public class LoadingCache {

    // get(K): V — the loaded value is derived from the (tainted) key.
    public Object get(Object key) {
        if (OpentaintNdUtil.nextBool()) {
            return null;
        }
        return key;
    }
}
