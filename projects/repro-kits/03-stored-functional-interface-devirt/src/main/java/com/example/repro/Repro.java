package com.example.repro;

import com.standin.cache.Cache;
import com.standin.cache.Loader;

/**
 * Minimal reproduction of the OpenTaint engine limitation
 * "stored functional-interface devirtualization via a static field".
 *
 * Mirrors the real jackson-jq flow in Conductor:
 *
 *   JsonJqTransform.createQueryCache():  Caffeine.newBuilder().build(JsonQuery::compile)  (BIND, field initializer)
 *   JsonJqTransform.start():             queryCache.get(queryExpression)                  (USE, another method)
 *
 * The bound loader is a functional interface stored inside the cache; {@code get()} invokes
 * it through that stored field. On a whole-program scan the indirect call fails to
 * devirtualize to the concrete bound loader (it resolves to the abstract interface method
 * and is dropped), so taint never enters the loader body -- a FALSE NEGATIVE.
 */
public final class Repro {

    private Repro() {
    }

    /** Taint SOURCE. */
    public static Object source() {
        return "tainted";
    }

    /** Taint SINK. */
    public static void sink(Object value) {
    }

    // ----------------------------------------------------------------------------------
    // (A) BUG flow: the loader is a STORED functional interface, invoked through the field
    //     inside get(). The bind happens in a static field initializer (one context, like
    //     createQueryCache()); the use happens in bugFlow() (another context, like start()).
    // ----------------------------------------------------------------------------------

    /** BIND: the concrete loader (with the sink INSIDE its body) is stored in the cache. */
    private static final Cache QUERY_CACHE = Cache.build(key -> {
        sink(key);          // <-- sink lives INSIDE the loader body (like JsonQuery::compile)
        return key;
    });

    /** USE: source flows into get(); get() must devirtualize the stored loader to reach sink. */
    public static void bugFlow() {
        QUERY_CACHE.get(source());
    }

    // ----------------------------------------------------------------------------------
    // (B) Aggravator: a SECOND, unrelated caller binds a DIFFERENT loader into the same
    //     shared static slot the approximation uses (mirrors LocalOnlyLock building its
    //     own Caffeine cache). This pollutes the flow-insensitive points-to on the slot.
    // ----------------------------------------------------------------------------------

    private static final Cache OTHER_CACHE = Cache.build(key -> new Object()); // unrelated loader

    public static void pollute() {
        OTHER_CACHE.get(new Object());
    }

    // ----------------------------------------------------------------------------------
    // (C) CONTROL flow: the SAME loader shape, but invoked DIRECTLY on a LOCAL variable --
    //     no field bridge, no external get(). Local points-to devirtualizes the SAM to the
    //     lambda, so taint reaches the sink. This is the contrast that isolates the field
    //     bridge as the cause of the bug-flow false negative.
    // ----------------------------------------------------------------------------------

    public static void controlDirectLambda() {
        Loader loader = key -> {
            sink(key);          // same sink-in-loader shape as the bug flow
            return key;
        };
        loader.load(source());  // direct local invocation -> should FIRE
    }

    // ----------------------------------------------------------------------------------
    // Baseline control: trivial source -> sink. Guaranteed to fire; proves the rule works.
    // ----------------------------------------------------------------------------------

    public static void controlPlain() {
        sink(source());
    }

    public static void main(String[] args) {
        bugFlow();
        pollute();
        controlDirectLambda();
        controlPlain();
    }
}
