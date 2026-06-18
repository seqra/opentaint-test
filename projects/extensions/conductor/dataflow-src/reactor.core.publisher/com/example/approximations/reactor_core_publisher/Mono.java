package com.example.approximations.reactor_core_publisher;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Dataflow approximation for reactor.core.publisher.Mono.
 *
 * Reactor's Mono carries (at most) one emitted element. The element is the
 * unit of taint: operators that run the emitted value through a user
 * function/supplier re-emit the (possibly transformed) value, so taint on the
 * upstream element reaches the downstream Mono's element.
 *
 *   - map(Function): apply the mapper to the upstream element, re-emit the
 *     mapper's result. this-element -> fn -> result-element.
 *   - defer(Supplier<Mono>): the supplier is invoked at subscription time and
 *     returns the Mono whose element is emitted. supplier-result-element ->
 *     result-element.
 *
 * Wrapper-returning shape: declare the concrete Mono return type, return `self`
 * on the nd branch (never null, which would drop the container taint), and
 * extract (block) -> apply -> re-wrap (just) so a downstream block()/etc. can
 * pull the tainted element back out.
 */
@Approximate(reactor.core.publisher.Mono.class)
public class Mono {

    // map(Function): upstream element -> mapper -> downstream element.
    public reactor.core.publisher.Mono map(@ArgumentTypeContext Function fn) throws Throwable {
        reactor.core.publisher.Mono self = (reactor.core.publisher.Mono) (Object) this;
        if (OpentaintNdUtil.nextBool()) return self;
        Object up = self.block();
        return reactor.core.publisher.Mono.justOrEmpty(fn.apply(up));
    }

    // defer(Supplier<Mono>): supplier returns the Mono whose element is emitted.
    public static reactor.core.publisher.Mono defer(@ArgumentTypeContext Supplier supplier) throws Throwable {
        Object supplied = supplier.get();
        if (supplied instanceof reactor.core.publisher.Mono) {
            return (reactor.core.publisher.Mono) supplied;
        }
        return reactor.core.publisher.Mono.justOrEmpty(supplied);
    }
}
