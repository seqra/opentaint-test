package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Dataflow approximations for org.jsoup.nodes.Element DOM-query methods that
 * return collections derived from the receiver element tree. Taint on the
 * receiver (this) flows into each element of the returned container.
 */
@Approximate(org.jsoup.nodes.Element.class)
public class Element {

    // Element#select(String): the matched Elements are descendants of this
    // element tree -> propagate this into each element of the result.
    public org.jsoup.select.Elements select(String cssQuery) {
        org.jsoup.nodes.Element self = (org.jsoup.nodes.Element) (Object) this;
        org.jsoup.select.Elements result = new org.jsoup.select.Elements();
        result.add(self);
        return result;
    }

    // Element#children(): child Elements of this element tree.
    public org.jsoup.select.Elements children() {
        org.jsoup.nodes.Element self = (org.jsoup.nodes.Element) (Object) this;
        org.jsoup.select.Elements result = new org.jsoup.select.Elements();
        result.add(self);
        return result;
    }

    // Element#textNodes(): the text nodes carry this element tree's text.
    public List<org.jsoup.nodes.TextNode> textNodes() {
        org.jsoup.nodes.Element self = (org.jsoup.nodes.Element) (Object) this;
        List result = new ArrayList();
        result.add(self);
        return result;
    }
}
