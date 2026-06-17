package com.example.approximations.java_util_stream;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.IntFunction;
import java.util.stream.Stream;

@Approximate(java.util.stream.IntStream.class)
public class IntStream {

    // Model: each int element flows through the IntFunction; the produced object
    // becomes an element of the returned Stream. We apply the mapper to a sample
    // element and wrap the result so a downstream Stream operation can pull it out.
    public Stream mapToObj(@ArgumentTypeContext IntFunction mapper) {
        java.util.stream.IntStream self = (java.util.stream.IntStream) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            return Stream.empty();
        }
        Object mapped = mapper.apply(self.findFirst().orElse(0));
        return Stream.of(mapped);
    }
}
