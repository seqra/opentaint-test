package repro;

import java.lang.reflect.Method;

/**
 * Two copies of the SAME source-&gt;sink flow:
 *
 *   Repro.source()  ->  Bean.handle(p)  ->  Repro.sink(p)
 *
 * {@link #directControl()} reaches the sink with a plain virtual call (no
 * reflection). {@link #reflectivePath()} reaches it through
 * {@link java.lang.reflect.Method#invoke(Object, Object[])}.
 *
 * The only difference between the two is the dispatch mechanism, so a taint
 * engine that models reflective dispatch must flag BOTH. OpenTaint flags only
 * the direct control: it builds no call edge from the {@code invoke} site to
 * the resolved target method, so the tainted {@code args[0]} never enters
 * {@code Bean#handle} and the reflective flow is a false negative.
 */
public class Main {

    /**
     * CONTROL (no reflection): must fire.
     * Repro.source() -> bean.handle(p) -> Repro.sink(p).
     */
    public static void directControl() {
        Bean bean = new Bean();
        bean.handle(Repro.source());
    }

    /**
     * REFLECTIVE (Method#invoke): should fire but does NOT (false negative).
     * The target is a concrete method on a concrete class, resolved by a
     * constant-name getMethod, so the reflective target is fully pinnable.
     */
    public static void reflectivePath() throws Throwable {
        Object[] args = new Object[] {Repro.source()}; // tainted element in the args array
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("handle", String.class); // resolvable target
        m.invoke(bean, args); // java.lang.reflect.Method#invoke(Object, Object[])
    }

    public static void main(String[] argv) throws Throwable {
        directControl();
        reflectivePath();
    }
}
