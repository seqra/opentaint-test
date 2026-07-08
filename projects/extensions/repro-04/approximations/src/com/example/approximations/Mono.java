package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;

/**
 * Minimal dataflow model for reactor.core.publisher.Mono#flatMap, mirroring the Halo
 * reactor-core-publisher approximation. It routes the receiver's element value into the mapper's
 * lambda parameter and flattens the returned Mono. The model is correct: the
 * "reactorFlatMapParamUsedDirectly" sample flags on both analyzer revisions; only the
 * captured-into-nested-flatMap sample regresses, which is the reproduced gap.
 *
 * <p>Mono#just and Mono#block are built-in models — this is the only custom approximation the kit
 * needs. Passed to the scan via {@code --dataflow-approximations
 * {ext}/repro-04/approximations/src}, compiled by OpenTaint at scan time.
 */
@Approximate(reactor.core.publisher.Mono.class)
public class Mono {

    private reactor.core.publisher.Mono self() {
        return (reactor.core.publisher.Mono) (Object) this;
    }

    public reactor.core.publisher.Mono flatMap(@ArgumentTypeContext Function fn) throws Throwable {
        reactor.core.publisher.Mono self = self();
        if (OpentaintNdUtil.nextBool()) return self;
        Object up = self.block();
        return (reactor.core.publisher.Mono) (Object) fn.apply(up);
    }
}
