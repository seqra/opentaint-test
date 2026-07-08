package repro;

/**
 * Taint model for the reproduction: a source and a sink the OpenTaint rules
 * key off of. JDK-only, no external dependencies.
 */
public final class Repro {

    private Repro() {
    }

    /** Taint SOURCE: the return value is treated as untrusted by the rule. */
    public static String source() {
        return "";
    }

    /** Taint SINK: any value reaching here is a finding. */
    public static void sink(Object value) {
        // no-op; presence of the call is what the rule matches
    }
}
