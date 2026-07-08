package app;

/**
 * Generic taint marker: {@code source()} returns tainted data, {@code sink(String)} is the
 * dangerous consumer. The detection rule (rules/approximation-rule.yaml) is
 * {@code app.Taint.source() -> app.Taint.sink($VALUE)}.
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
