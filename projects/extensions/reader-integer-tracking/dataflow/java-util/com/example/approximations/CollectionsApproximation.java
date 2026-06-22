package com.example.approximations;

import java.util.Comparator;
import java.util.List;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(java.util.Collections.class)
public class CollectionsApproximation {
    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        T left = element(list);
        T right = element(list);
        if (left != null) {
            left.compareTo(right);
        }
    }

    public static <T> void sort(List<T> list, @ArgumentTypeContext Comparator<? super T> comparator) {
        T left = element(list);
        T right = element(list);
        if (comparator != null) {
            comparator.compare(left, right);
        } else if (left instanceof Comparable) {
            ((Comparable) left).compareTo(right);
        }
    }

    private static <T> T element(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1 && OpentaintNdUtil.nextBool()) {
            return list.get(1);
        }
        return list.get(0);
    }
}
