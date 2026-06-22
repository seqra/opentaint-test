package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

/**
 * Models org.seimicrawler.xpath.JXDocument#selN.
 *
 * selN runs an xpath query against the receiver document and returns a
 * List<JXNode> whose elements are derived from that (tainted) DOM. Container
 * element propagation: taint on the receiver document flows into every element
 * of the returned list, where a downstream get(i)/value() reads it back out.
 *
 * Modelled by routing the receiver into the returned list element so that
 * List.get(...) hands the tainted receiver back to the caller.
 */
@Approximate(org.seimicrawler.xpath.JXDocument.class)
public class JXDocument {

    // create wraps the supplied HTML string / jsoup Document / Elements into the
    // returned JXDocument: arg0 -> result.
    public static org.seimicrawler.xpath.JXDocument create(String html) {
        return (org.seimicrawler.xpath.JXDocument) (Object) html;
    }

    public static org.seimicrawler.xpath.JXDocument create(org.jsoup.nodes.Document doc) {
        return (org.seimicrawler.xpath.JXDocument) (Object) doc;
    }

    public static org.seimicrawler.xpath.JXDocument create(org.jsoup.select.Elements elements) {
        return (org.seimicrawler.xpath.JXDocument) (Object) elements;
    }

    public java.util.List selN(String xpath) {
        org.seimicrawler.xpath.JXDocument self =
                (org.seimicrawler.xpath.JXDocument) (Object) this;
        java.util.List result = new java.util.ArrayList();
        if (OpentaintNdUtil.nextBool()) {
            result.add(self);
        }
        return result;
    }
}
