package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Dataflow approximation for org.jsoup.select.Elements. select narrows the
 * receiver element collection into a new matched Elements container; taint on
 * the receiver's elements (this) flows into the returned container (result).
 */
@Approximate(org.jsoup.select.Elements.class)
public class Elements {

    // Elements#select(String): the matched Elements are derived from the
    // receiver collection -> this -> result.
    public org.jsoup.select.Elements select(String cssQuery) {
        org.jsoup.select.Elements self = (org.jsoup.select.Elements) (Object) this;
        if (OpentaintNdUtil.nextBool()) return self;
        org.jsoup.select.Elements result = new org.jsoup.select.Elements();
        result.addAll(self);
        return result;
    }
}
