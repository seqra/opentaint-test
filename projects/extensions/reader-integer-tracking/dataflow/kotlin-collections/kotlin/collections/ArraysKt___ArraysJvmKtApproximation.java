package kotlin.collections;

import java.util.Comparator;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;

@Approximate(ArraysKt___ArraysJvmKt.class)
public class ArraysKt___ArraysJvmKtApproximation {
    public static <T> void sortWith(T[] values, @ArgumentTypeContext Comparator<? super T> comparator) {
        if (values == null || values.length == 0 || comparator == null) {
            return;
        }

        T value = values[0];
        comparator.compare(value, value);
    }

    public static <T> void sortWith(T[] values, @ArgumentTypeContext Comparator<? super T> comparator, int fromIndex, int toIndex) {
        if (values == null || values.length == 0 || comparator == null) {
            return;
        }

        T value = values[0];
        comparator.compare(value, value);
    }
}
