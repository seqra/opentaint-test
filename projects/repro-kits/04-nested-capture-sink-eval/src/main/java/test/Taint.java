package test;

/**
 * Generic taint markers. {@link #source()} is the untrusted-data source and {@link #sink(String)}
 * the sensitive sink, matched by the rule in
 * projects/extensions/repro-04/rules/nested-capture-source-to-sink.yaml.
 */
public final class Taint {

    private Taint() {
    }

    public static String source() {
        return "";
    }

    public static void sink(String value) {
    }
}
