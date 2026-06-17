package com.example.approximations.java_util_stream;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@Approximate(java.util.stream.Collectors.class)
public class Collectors {

    // Build a Collector whose supplier returns the already-populated (possibly
    // tainted) container, so that Stream.collect surfaces the tainted result.
    private static Collector carrying(final Object container) {
        Supplier supplier = new Supplier() {
            public Object get() {
                return container;
            }
        };
        BiConsumer accumulator = new BiConsumer() {
            public void accept(Object a, Object b) {
            }
        };
        BinaryOperator combiner = new BinaryOperator() {
            public Object apply(Object a, Object b) {
                return a;
            }
        };
        return Collector.of(supplier, accumulator, combiner);
    }

    // Model: the value function's result becomes a map value; the key function's
    // result becomes a map key. Apply both to a sample element so taint on the
    // captured data flows through the lambdas into the collected Map.
    public static Collector toMap(@ArgumentTypeContext Function keyMapper,
                                  @ArgumentTypeContext Function valueMapper) {
        Map result = new HashMap();
        Object element = OpentaintNdUtil.nextBool() ? null : "";
        result.put(keyMapper.apply(element), valueMapper.apply(element));
        return carrying(result);
    }

    public static Collector toMap(@ArgumentTypeContext Function keyMapper,
                                  @ArgumentTypeContext Function valueMapper,
                                  @ArgumentTypeContext BinaryOperator mergeFunction) {
        return toMap(keyMapper, valueMapper);
    }

    public static Collector toMap(@ArgumentTypeContext Function keyMapper,
                                  @ArgumentTypeContext Function valueMapper,
                                  @ArgumentTypeContext BinaryOperator mergeFunction,
                                  @ArgumentTypeContext Supplier mapSupplier) {
        Map result = (Map) mapSupplier.get();
        Object element = OpentaintNdUtil.nextBool() ? null : "";
        result.put(keyMapper.apply(element), valueMapper.apply(element));
        return carrying(result);
    }

    // Model: the classifier's result becomes a map key; each element is grouped
    // into the list value under its key.
    public static Collector groupingBy(@ArgumentTypeContext Function classifier) {
        Map result = new HashMap();
        Object element = OpentaintNdUtil.nextBool() ? null : "";
        Object key = classifier.apply(element);
        List bucket = new ArrayList();
        bucket.add(element);
        result.put(key, bucket);
        return carrying(result);
    }

    // Model: elements flow into the collection produced by the supplier.
    public static Collector toCollection(@ArgumentTypeContext Supplier collectionFactory) {
        Object result = collectionFactory.get();
        return carrying(result);
    }
}
