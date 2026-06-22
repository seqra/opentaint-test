package kotlin.collections;

import java.util.Iterator;
import java.util.List;

import kotlin.jvm.functions.Function1;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(CollectionsKt__MutableCollectionsKt.class)
public class CollectionsKt__MutableCollectionsKtApproximation {
    public static <T> boolean removeAll(Iterable<? extends T> values, @ArgumentTypeContext Function1<? super T, Boolean> predicate) {
        if (predicate == null) {
            return false;
        }

        predicate.invoke(first(values));
        return OpentaintNdUtil.nextBool();
    }

    public static <T> boolean removeAll(List<T> values, @ArgumentTypeContext Function1<? super T, Boolean> predicate) {
        if (predicate == null) {
            return false;
        }

        T value = null;
        if (values != null && !values.isEmpty()) {
            value = values.get(0);
        }
        predicate.invoke(value);
        return OpentaintNdUtil.nextBool();
    }

    private static <T> T first(Iterable<? extends T> values) {
        if (values == null) {
            return null;
        }

        Iterator<? extends T> iterator = values.iterator();
        if (iterator == null || !iterator.hasNext()) {
            return null;
        }
        return iterator.next();
    }
}
