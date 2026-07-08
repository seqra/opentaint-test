package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;

/**
 * Dataflow approximation for the DISTINCT stand-in external builder {@code lib.Y#of(Function)}.
 *
 * Structurally identical to the X.of approximation, but for a different method. It exists so the
 * CONTROL flow ({@code X.of -> Y.of}) nests a DISTINCT approximation method inside X.of. Because
 * Y.of is not already on the approximation call stack when it is dispatched, its captured lambda
 * IS applied and the inner sink fires -- proving the failure is same-method re-entry, not depth.
 */
@Approximate(lib.Y.class)
public class Y {

    public static lib.Y of(@ArgumentTypeContext Function fn) throws Throwable {
        lib.YB b = new lib.YB();
        Object built = fn.apply(b);
        lib.ObjectBuilder ob = (lib.ObjectBuilder) built;
        return (lib.Y) ob.build();
    }
}
