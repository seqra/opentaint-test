package repro;

/**
 * Concrete reflective target. Its {@link #handle(String)} method is the callee
 * that {@code Method#invoke} is expected to dispatch into. It reads its
 * parameter and passes it straight to the sink, so any taint on the parameter
 * is an immediate source-&gt;sink flow.
 */
public class Bean {

    public void handle(String p) {
        Repro.sink(p);
    }
}
