package com.example.approximations.com_google_common;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.concurrent.Callable;

@Approximate(com.google.common.cache.Cache.class)
public class Cache {

    // Model: Cache.get(key, valueLoader) — on a cache miss the valueLoader Callable
    // is invoked, its result is stored under the key and returned. We route taint
    // from the Callable's result to the returned value (and store it back so a later
    // getIfPresent under the same key observes it too).
    public Object get(Object key, @ArgumentTypeContext Callable valueLoader) throws Throwable {
        com.google.common.cache.Cache self = (com.google.common.cache.Cache) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            // Cache hit: return the already-cached value for this key.
            return self.getIfPresent(key);
        }
        // Cache miss: load the value, cache it, and return it.
        Object loaded = valueLoader.call();
        self.put(key, loaded);
        return loaded;
    }
}
