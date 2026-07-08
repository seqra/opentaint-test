package app;

import lib.X;
import lib.Y;

/**
 * Two flows that differ ONLY in whether the inner {@code *.of(Function)} invoker is the SAME
 * approximation method as the outer one. In both, the source is captured in the innermost
 * lambda and the sink sits inside that same innermost lambda, so the only variable is whether
 * that inner lambda gets dispatched.
 *
 * Expected: BOTH sinks fire (both inner lambdas are dispatched by their modeled *.of).
 * Actual (the bug): only the CONTROL fires; the RECURSIVE case does not, because OpenTaint
 * cuts the inner X.of dispatch when X.of is already on the approximation call stack.
 */
public class Samples {

    /**
     * RECURSIVE / SAME-METHOD case: X.of nested inside X.of.
     *
     *   X.of(a -> a.nest( X.of(b -> { sink(taint); return b; }) ))
     *
     * The outer X.of dispatches (X.of not yet on the approximation stack) and its lambda body
     * is analyzed. Inside it, the inner X.of is called -- but X.of is now already on the
     * approximation stack, so the recursion guard cuts the inner fn.apply. The inner lambda
     * body {@code b -> { sink(taint); ... }} is never analyzed as invoked, so the sink is
     * never reached: a FALSE NEGATIVE.
     */
    public void recursiveSameMethod() {
        String taint = Taint.source();
        X.of(a -> a.nest(
            X.of(b -> {
                Taint.sink(taint);   // <-- inner (X.of) lambda: NOT dispatched -> false negative
                return b;
            })
        ));
    }

    /**
     * CONTROL / DISTINCT-METHOD case: Y.of nested inside X.of (identical nesting depth).
     *
     *   X.of(a -> a.nestY( Y.of(b -> { sink(taint); return b; }) ))
     *
     * The outer X.of dispatches; inside its body the inner invoker is Y.of, a DIFFERENT
     * approximation method, so it is NOT on the approximation stack and dispatches normally.
     * The inner lambda body is analyzed and the sink fires: TRUE POSITIVE. Same depth as the
     * recursive case, so the only difference is same-method vs distinct-method re-entry.
     */
    public void controlDistinctMethod() {
        String taint = Taint.source();
        X.of(a -> a.nestY(
            Y.of(b -> {
                Taint.sink(taint);   // <-- inner (Y.of) lambda: dispatched -> true positive
                return b;
            })
        ));
    }
}
