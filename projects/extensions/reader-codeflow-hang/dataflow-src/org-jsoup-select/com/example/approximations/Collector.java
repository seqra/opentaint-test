package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for org.jsoup.select.Collector. collect gathers the
 * matching descendants of the root element (arg1) into the returned Elements
 * container; taint on the root element flows into the result collection
 * (arg1 -> result).
 */
@Approximate(org.jsoup.select.Collector.class)
public class Collector {

    // Collector#collect(Evaluator, Element): matched descendants of root are
    // gathered into the result -> arg1 (root) -> result.
    public static org.jsoup.select.Elements collect(
            org.jsoup.select.Evaluator eval, org.jsoup.nodes.Element root) {
        org.jsoup.select.Elements result = new org.jsoup.select.Elements();
        if (OpentaintNdUtil.nextBool()) {
            result.add(root);
        }
        return result;
    }
}
