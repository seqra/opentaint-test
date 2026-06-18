package com.example.approximations.rx;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for rx.Observable (RxJava 1.2.2).
 *
 * Models the reactive callback chain by which a tainted emission travels from
 * the producer side (the OnSubscribe handed to create) to the consumer side
 * (the Action1 onNext handed to subscribe):
 *
 *   create(OnSubscribe f) -> Observable (arg(0) -> return): the OnSubscribe
 *     producer carries the tainted emission; the returned Observable is modeled
 *     to carry it. We both drive the producer with a capturing subscriber and
 *     fall back to treating the producer reference itself as the Observable
 *     identity, so whichever way the taint sits it reaches the return.
 *
 *   subscribe(Action1 onNext) / subscribe(Action1 onNext, Action1 onError)
 *     (this -> arg(0), and arg(1) on the error path): the Observable (this)
 *     drives its producer with a forwarding subscriber and/or forwards its own
 *     value directly into the consumer's onNext, so the emission reaches the
 *     subscriber callback.
 *
 * Approximation class lives in package com.example.approximations (NOT
 * rx) — naming it with the target FQN breaks Approximations.methodsOf.
 *
 * KNOWN LIMITATION (tests_passing: blocked): when the tainted value is produced
 * INSIDE the OnSubscribe callback and emitted via subscriber.onNext(...) — the
 * idiomatic RxJava / conductor *ObservableQueue shape — the analyzer does not
 * carry that emission across the create -> subscribe boundary into the consumer
 * Action1. Each hop verifies in isolation (this->onNext; arg(0)->return for a
 * directly-tainted producer reference), but the end-to-end producer-emission
 * flow is dropped engine-side (the "callback-through-returned-object"
 * limitation noted in the skill).
 */
@Approximate(rx.Observable.class)
public class Observable {

    // A Subscriber whose onNext forwards the producer's emission straight into
    // the consumer's Action1 (collapses producer -> consumer into one hop).
    private static final class ForwardingSubscriber extends rx.Subscriber<Object> {
        private final rx.functions.Action1 onNext;

        ForwardingSubscriber(rx.functions.Action1 onNext) {
            this.onNext = onNext;
        }

        @Override
        public void onNext(Object t) {
            if (onNext != null) {
                onNext.call(t);
            }
        }

        @Override
        public void onError(Throwable e) {
        }

        @Override
        public void onCompleted() {
        }
    }

    // A Subscriber that captures the single emission produced by an OnSubscribe.
    private static final class CapturingSubscriber extends rx.Subscriber<Object> {
        Object captured;

        @Override
        public void onNext(Object t) {
            this.captured = t;
        }

        @Override
        public void onError(Throwable e) {
        }

        @Override
        public void onCompleted() {
        }
    }

    // create(OnSubscribe f) -> Observable; arg(0) (the producer) -> return.
    // Drive the producer here (where its concrete type is known via
    // @ArgumentTypeContext) with a capturing subscriber, then return the
    // captured tainted emission as the Observable identity so it carries the
    // producer's emission. subscribe(this) then forwards it to the consumer.
    public static rx.Observable create(@ArgumentTypeContext rx.Observable.OnSubscribe f) {
        CapturingSubscriber sub = new CapturingSubscriber();
        if (f != null) {
            f.call(sub);
        }
        if (OpentaintNdUtil.nextBool()) {
            return (rx.Observable) sub.captured;
        }
        return (rx.Observable) (Object) f;
    }

    // subscribe(Action1 onNext) -> Subscription; this (the Observable produced
    // by create) IS the OnSubscribe; driving it emits the tainted value into a
    // subscriber that forwards straight to the Action1 (this -> arg(0)).
    public rx.Subscription subscribe(@ArgumentTypeContext rx.functions.Action1 onNext) {
        if (OpentaintNdUtil.nextBool()) {
            deliver(this, onNext);
        }
        return null;
    }

    // subscribe(Action1 onNext, Action1 onError) -> Subscription; this -> arg(0).
    public rx.Subscription subscribe(@ArgumentTypeContext rx.functions.Action1 onNext,
                                     @ArgumentTypeContext rx.functions.Action1 onError) {
        if (OpentaintNdUtil.nextBool()) {
            deliver(this, onNext);
        } else if (onError != null) {
            onError.call(new RuntimeException());
        }
        return null;
    }

    // Drives the OnSubscribe held by the Observable (this) with a forwarding
    // subscriber, so the producer's tainted emission reaches the consumer.
    // Also forwards the Observable's own taint (this -> arg) directly, so the
    // emission reaches the consumer even when the producer.call hop is opaque.
    private static void deliver(Object self, rx.functions.Action1 onNext) {
        if (onNext == null) {
            return;
        }
        if (OpentaintNdUtil.nextBool()) {
            onNext.call(self);
        } else {
            rx.Observable.OnSubscribe producer = (rx.Observable.OnSubscribe) self;
            ForwardingSubscriber sub = new ForwardingSubscriber(onNext);
            producer.call(sub);
        }
    }
}
