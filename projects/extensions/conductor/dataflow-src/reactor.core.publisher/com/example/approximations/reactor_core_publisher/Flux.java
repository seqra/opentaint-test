package com.example.approximations.reactor_core_publisher;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Dataflow approximation for reactor.core.publisher.Flux.
 *
 * Reactor's Flux carries a stream of emitted elements; each element is the unit
 * of taint. Operators that run the emitted value through a user function or
 * predicate re-emit the (possibly transformed / filtered) value, so taint on an
 * upstream element reaches the downstream Flux's elements.
 *
 *   - map(Function): apply the mapper to an emitted element, re-emit the
 *     mapper's result. this-element -> fn -> result-element.
 *   - filter(Predicate): run the predicate over an emitted element and re-emit
 *     the same element when it passes. The predicate does not transform the
 *     value, so the upstream element flows straight through to the downstream
 *     Flux. (The predicate is still invoked so any flow into the predicate is
 *     modelled.)
 *
 * Wrapper-returning shape: declare the concrete Flux return type, return `self`
 * on the nd branch (never null), and extract (blockFirst) -> apply -> re-wrap
 * (just) so a downstream blockFirst()/etc. surfaces the tainted element.
 */
@Approximate(reactor.core.publisher.Flux.class)
public class Flux {

    // map(Function): upstream element -> mapper -> downstream element.
    public reactor.core.publisher.Flux map(@ArgumentTypeContext Function fn) throws Throwable {
        reactor.core.publisher.Flux self = (reactor.core.publisher.Flux) (Object) this;
        if (OpentaintNdUtil.nextBool()) return self;
        Object up = self.blockFirst();
        return reactor.core.publisher.Flux.just(fn.apply(up));
    }

    // filter(Predicate): element flows through unchanged when it passes.
    public reactor.core.publisher.Flux filter(@ArgumentTypeContext Predicate predicate) throws Throwable {
        reactor.core.publisher.Flux self = (reactor.core.publisher.Flux) (Object) this;
        if (OpentaintNdUtil.nextBool()) return self;
        Object up = self.blockFirst();
        predicate.test(up);
        return reactor.core.publisher.Flux.just(up);
    }
}
