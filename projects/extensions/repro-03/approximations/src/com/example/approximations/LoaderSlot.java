package com.example.approximations;

/**
 * Global static slot bridging {@code Cache.build(loader)} (which BINDS the loader) and
 * {@code Cache.get(key)} (which APPLIES it).
 *
 * The two calls are joined only by the Cache object. build() publishes the loader here;
 * get() reads it back and applies it to the key. The loader's concrete identity (a lambda
 * / method reference) is supposed to travel through this field by flow-insensitive
 * points-to so that get's {@code loader.load(key)} dispatches to the real bound loader.
 *
 * This mirrors the project's
 * {@code .opentaint/dataflow/com-github-benmanes-caffeine-cache/.../CacheLoaderSlot.java}.
 */
final class LoaderSlot {

    /** The loader bound by the most recent Cache.build(Loader). */
    static com.standin.cache.Loader loader;

    private LoaderSlot() {
    }
}
