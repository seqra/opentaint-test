package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for the stand-in external class {@code com.standin.cache.Cache}.
 *
 * This is the exact field-bridge pattern the project uses for Caffeine
 * ({@code .opentaint/dataflow/com-github-benmanes-caffeine-cache/}), collapsed to one
 * class because {@code build} and {@code get} both live on {@code Cache}:
 *
 *   - build(@ArgumentTypeContext Loader loader): publishes the loader into a shared static
 *     slot ({@link LoaderSlot#loader}). The @ArgumentTypeContext parameter is meant to
 *     carry the loader's concrete method-ref/lambda identity into the slot.
 *   - get(Object key): reads the slot back and calls loader.load(key). The load hop is left
 *     as a REAL invocation so the engine can devirtualize it to the concrete bound loader
 *     (this is the call the engine FAILS to devirtualize on a whole-program scan -- it
 *     resolves to the abstract com.standin.cache.Loader#load and drops the taint).
 *
 * NOTE: approximating Loader#load directly is NOT an option -- it would intercept and
 * replace the concrete loader body, dropping the sink INSIDE the loader. The load hop must
 * stay a real invocation and be devirtualized by the engine.
 */
@Approximate(com.standin.cache.Cache.class)
public class Cache {

    // build(Loader): bind the loader for a subsequent cache.get(key) to apply.
    public static com.standin.cache.Cache build(
            @ArgumentTypeContext com.standin.cache.Loader loader) {
        LoaderSlot.loader = loader;   // bind the loader to the returned cache
        return null;
    }

    // get(key): apply the bound loader to the key and return its result.
    public Object get(Object key) throws Throwable {
        com.standin.cache.Loader loader = LoaderSlot.loader;
        if (loader == null || OpentaintNdUtil.nextBool()) {
            return null;
        }
        return loader.load(key);   // key -> load argument; load result -> get result
    }
}
