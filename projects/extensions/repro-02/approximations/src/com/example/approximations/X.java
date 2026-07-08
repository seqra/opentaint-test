package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for the stand-in external builder {@code lib.X#of(Function)}.
 *
 * Real flow:
 *   public static X of(Function<XB, ObjectBuilder<X>> fn) { return fn.apply(new XB()).build(); }
 *
 * The analyzer drops the external of(Function), so a from->to copy cannot express applying the
 * captured Function. Modeled as code: invoke fn against a fresh, CONCRETE XB (forward handoff
 * inside the same method), then build and return X so a downstream consumer can pull the value.
 *
 * This is the SAME approximation method dispatched twice when the app nests X.of inside X.of --
 * the second (inner) dispatch is what the engine cuts.
 */
@Approximate(lib.X.class)
public class X {

    public static lib.X of(@ArgumentTypeContext Function fn) throws Throwable {
        lib.XB b = new lib.XB();
        Object built = fn.apply(b);
        lib.ObjectBuilder ob = (lib.ObjectBuilder) built;
        return (lib.X) ob.build();
    }
}
