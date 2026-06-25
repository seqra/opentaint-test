package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for the kotlinx.coroutines coroutine builder.
 *
 * `scope.launch(Dispatchers.IO) { ... }` lowers to a static call
 *   kotlinx.coroutines.BuildersKt.launch$default(scope, ctx, start, Function2(SuspendLambda), flags, null)
 * (and, for the non-default overload, BuildersKt.launch(scope, ctx, start, Function2)).
 * The block body — which dispatches to controller methods / sources / sinks — lives in the
 * SuspendLambda's invokeSuspend, reached via Function2#invoke(p1, p2).
 *
 * The engine drops these as unmodeled higher-order externals and never invokes the Function2
 * block, so any source->sink flow living inside `launch { ... }` is unreachable (same gap class as
 * Route#handler, which we model by calling handler.handle(...)). Here we model the builders as
 * INVOKING block.invoke(scope, null): p1 = the CoroutineScope receiver, p2 = the Continuation
 * (null is fine — source/sink rules are syntactic, only reachability matters). This routes into the
 * SuspendLambda's invokeSuspend, which runs the block body, and also makes captured/synthetic-field
 * taint that crosses the closure capture reachable.
 *
 * Function2 has `Object invoke(Object p1, Object p2)`. These builders are STATIC, so there is no
 * `this`; the receiver scope is the first argument. The Job return value is not modeled (return null).
 */
@Approximate(kotlinx.coroutines.BuildersKt.class)
public class BuildersKt {

    // launch$default(CoroutineScope, CoroutineContext, CoroutineStart, Function2, int, Object) -> Job
    public static kotlinx.coroutines.Job launch$default(
            kotlinx.coroutines.CoroutineScope scope,
            kotlin.coroutines.CoroutineContext context,
            kotlinx.coroutines.CoroutineStart start,
            @ArgumentTypeContext kotlin.jvm.functions.Function2 block,
            int flags,
            Object marker) throws Throwable {
        if (OpentaintNdUtil.nextBool()) {
            block.invoke(scope, null);
        }
        return null;
    }

    // launch(CoroutineScope, CoroutineContext, CoroutineStart, Function2) -> Job
    public static kotlinx.coroutines.Job launch(
            kotlinx.coroutines.CoroutineScope scope,
            kotlin.coroutines.CoroutineContext context,
            kotlinx.coroutines.CoroutineStart start,
            @ArgumentTypeContext kotlin.jvm.functions.Function2 block) throws Throwable {
        if (OpentaintNdUtil.nextBool()) {
            block.invoke(scope, null);
        }
        return null;
    }
}
