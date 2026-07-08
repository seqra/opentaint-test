package test;

import reactor.core.publisher.Mono;

/**
 * Repro-kit 04 — a source-to-sink taint flow that is found by an older OpenTaint analyzer but lost
 * by a newer one, when the flow crosses a nested lambda whose capturing inner lambda is invoked by
 * a dataflow approximation ({@code Mono#flatMap}).
 *
 * <p>Four flows form a differential. Each routes {@code Taint.source()} to {@code Taint.sink(...)};
 * a correct engine flags all four. The first three (controls) flag on both analyzer revisions; only
 * {@link #reactorFlatMapParamCapturedIntoNestedFlatMap} regresses — it is flagged by analyzer
 * {@code 2026.06.26.4253df6} (semantic 0.4.3) and MISSED by {@code dev} / 0.4.4. The regression
 * signal is that finding-count delta.
 *
 * <p>The decisive pair is {@link #reactorFlatMapParamCapturedIntoRunnable} vs
 * {@link #reactorFlatMapParamCapturedIntoNestedFlatMap}: identical outer approximation
 * ({@code Mono#flatMap}), identical captured parameter {@code p}, differing only in whether the
 * inner capturing lambda is invoked by ordinary project code ({@code Runnable#run}, still flagged)
 * or by another approximation callback ({@code Mono#flatMap}, lost).
 *
 * <p>Reduced from run.halo.app.extension.router.ExtensionPatchHandler#handle, where the outer
 * {@code flatMap} parameter {@code jsonPatch} is captured into an inner
 * {@code client.getJsonExtension(...).flatMap(jsonExtension -> jsonPatch.apply(...))} — and the
 * CWE-15 JSON-Patch mass-assignment finding disappears on the newer analyzer.
 *
 * <p>Mechanism note: {@code test rule reachability} shows the taint facts reaching the sink are
 * byte-identical on both revisions, so the loss is in the newer engine's finding / sink evaluation
 * downstream of propagation, not a taint-propagation drop. The kit's value here is the observable
 * finding-count delta, independent of that root-cause detail.
 */
public class NestedCaptureSamples {

    // ---- control 1: plain tainted local captured into a nested lambda invoked by PROJECT code.
    //      No approximation callback involved. Flags on both revisions.

    interface Fn {
        void apply(String p);
    }

    private static void run(Fn f, String arg) {
        f.apply(arg);
    }

    public void pureJavaParamCapturedIntoNestedLambda() {
        run(p -> {
            Runnable r = () -> Taint.sink(p);
            r.run();
        }, Taint.source());
    }

    // ---- control 2: approximation-delivered param used DIRECTLY in the same lambda body.
    //      Flags on both revisions.

    public void reactorFlatMapParamUsedDirectly() {
        Mono.just(Taint.source())
                .flatMap(p -> {
                    Taint.sink(p);
                    return Mono.just(p);
                })
                .block();
    }

    // ---- control 3: approximation-delivered param captured into a nested lambda invoked by
    //      PROJECT code (Runnable#run in the outer body). Flags on both revisions — the capture
    //      itself is fine; what matters is who invokes the capturing lambda.

    public void reactorFlatMapParamCapturedIntoRunnable() {
        Mono.just(Taint.source())
                .flatMap(p -> {
                    Runnable r = () -> Taint.sink(p);
                    r.run();
                    return Mono.just(p);
                })
                .block();
    }

    // ---- REGRESSION: approximation-delivered param captured into a lambda invoked by ANOTHER
    //      approximation callback (the inner Mono#flatMap's fn.apply). Flagged on 0.4.3, MISSED on
    //      dev/0.4.4. Differs from control 3 only in the inner invoker (flatMap vs Runnable#run).

    public void reactorFlatMapParamCapturedIntoNestedFlatMap() {
        Mono.just(Taint.source())
                .flatMap(p -> Mono.just("clean")
                        .flatMap(clean -> {
                            Taint.sink(p);
                            return Mono.just(clean);
                        }))
                .block();
    }
}
