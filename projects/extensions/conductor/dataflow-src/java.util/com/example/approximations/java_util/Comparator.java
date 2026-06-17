package com.example.approximations.java_util;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

import java.util.function.Function;
import java.util.function.ToIntFunction;

@Approximate(java.util.Comparator.class)
public class Comparator {

    // Model: the returned Comparator routes each compared element through the
    // captured key-extractor Function, so element taint flows into arg(0) when
    // the comparator is later used (e.g. via compare()/sort()).
    //
    // NOTE: the analyzer's IFDS summaries do not follow a callback that is
    // invoked through a functional object RETURNED by an approximation at a
    // later, separate call site, so this element->keyExtractor flow is not
    // propagated in practice (verified: removing this class changes nothing).
    // The body still records the correct propagation intent.
    public static java.util.Comparator comparing(@ArgumentTypeContext final Function keyExtractor) {
        return (a, b) -> {
            keyExtractor.apply(a);
            keyExtractor.apply(b);
            return 0;
        };
    }

    public static java.util.Comparator comparingInt(@ArgumentTypeContext final ToIntFunction keyExtractor) {
        return (a, b) -> {
            keyExtractor.applyAsInt(a);
            keyExtractor.applyAsInt(b);
            return 0;
        };
    }
}
