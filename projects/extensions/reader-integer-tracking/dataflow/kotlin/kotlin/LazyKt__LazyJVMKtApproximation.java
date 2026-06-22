package kotlin;

import kotlin.jvm.functions.Function0;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

@Approximate(LazyKt__LazyJVMKt.class)
public class LazyKt__LazyJVMKtApproximation {
    public static Lazy lazy(@ArgumentTypeContext Function0 initializer) {
        return (Lazy) initializer.invoke();
    }

    public static Lazy lazy(LazyThreadSafetyMode mode, @ArgumentTypeContext Function0 initializer) {
        return (Lazy) initializer.invoke();
    }

    public static Lazy lazy(Object lock, @ArgumentTypeContext Function0 initializer) {
        return (Lazy) initializer.invoke();
    }
}
