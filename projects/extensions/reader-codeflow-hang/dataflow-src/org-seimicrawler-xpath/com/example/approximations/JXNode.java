package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Models org.seimicrawler.xpath.JXNode#sel.
 *
 * sel runs an xpath query against the receiver node tree and returns a
 * List<JXNode> whose elements are derived from that (tainted) sub-DOM.
 * Container element propagation: taint on the receiver node flows into every
 * element of the returned list, where a downstream get(i)/value() reads it out.
 *
 * Modelled by routing the receiver into the returned list element so that
 * List.get(...) hands the tainted receiver back to the caller.
 */
@Approximate(org.seimicrawler.xpath.JXNode.class)
public class JXNode {

    public java.util.List sel(String xpath) {
        org.seimicrawler.xpath.JXNode self =
                (org.seimicrawler.xpath.JXNode) (Object) this;
        java.util.List result = new java.util.ArrayList();
        if (OpentaintNdUtil.nextBool()) {
            result.add(self);
        }
        return result;
    }

    // value / asElement return the underlying value/element of the receiver
    // node: this -> result.
    public Object value() {
        return this;
    }

    public org.jsoup.nodes.Element asElement() {
        return (org.jsoup.nodes.Element) (Object) this;
    }
}
