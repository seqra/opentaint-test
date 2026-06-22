package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import kotlin.jvm.functions.Function1;

/**
 * Dataflow approximation for kotlin.collections.CollectionsKt#joinToString$default.
 *
 * joinToString folds the elements of the receiver Iterable (arg0) into a single
 * String. Each element is optionally mapped through the Function1 transform (arg6)
 * before being concatenated, so the element's taint must reach the returned String
 * either directly or through the transform lambda.
 *
 * The call site emitted by the Kotlin compiler references the public facade
 * kotlin.collections.CollectionsKt; the method is declared on the package-private
 * split kotlin.collections.CollectionsKt___CollectionsKt, which cannot be named
 * from outside its package, so the public facade is targeted here.
 */
@Approximate(kotlin.collections.CollectionsKt.class)
public class CollectionsKt {

    // signature:
    // (Ljava/lang/Iterable;Ljava/lang/CharSequence;Ljava/lang/CharSequence;
    //  Ljava/lang/CharSequence;ILjava/lang/CharSequence;
    //  Lkotlin/jvm/functions/Function1;ILjava/lang/Object;)Ljava/lang/String;
    public static String joinToString$default(
            Iterable iterable,
            CharSequence separator,
            CharSequence prefix,
            CharSequence postfix,
            int limit,
            CharSequence truncated,
            @ArgumentTypeContext Function1 transform,
            int mask,
            Object marker) {
        if (iterable == null) return null;
        Object element = iterable.iterator().next();
        if (transform != null && OpentaintNdUtil.nextBool()) {
            // element -> transform -> joined string
            return (String) (Object) transform.invoke(element);
        }
        // element -> joined string directly
        return (String) (Object) element;
    }
}
