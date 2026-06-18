package com.example.approximations.com_fasterxml_jackson;

import org.opentaint.ir.approximation.annotation.Approximate;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.AbstractMap;

/**
 * Dataflow approximation for com.fasterxml.jackson.databind.JsonNode.
 *
 * Models the methods whose taint flows through an iterator / container of
 * child nodes rather than a flat copy:
 *
 *   - elements(): Iterator<JsonNode> over the child nodes. Taint on the parent
 *     node (this) reaches every yielded child node.
 *   - fields(): Iterator<Map.Entry<String,JsonNode>> over (name, child) pairs.
 *     Taint on the parent node (this) reaches the value of every yielded entry.
 *
 * Each child node placed in the iterator is `this` itself, so the parent's
 * taint propagates to the elements; a downstream extractor (asText()) then
 * surfaces it. asText() is modelled as a this->return copy so the tainted
 * child node yields a tainted value.
 */
@Approximate(com.fasterxml.jackson.databind.JsonNode.class)
public class JsonNode {

    // elements() -> Iterator<JsonNode>; parent taint reaches each child node.
    public Iterator elements() {
        com.fasterxml.jackson.databind.JsonNode self =
            (com.fasterxml.jackson.databind.JsonNode) (Object) this;
        List<com.fasterxml.jackson.databind.JsonNode> children =
            new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        children.add(self);
        return children.iterator();
    }

    // fields() -> Iterator<Map.Entry<String,JsonNode>>; parent taint reaches
    // each entry value.
    public Iterator fields() {
        com.fasterxml.jackson.databind.JsonNode self =
            (com.fasterxml.jackson.databind.JsonNode) (Object) this;
        List<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> entries =
            new ArrayList<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>>();
        entries.add(new AbstractMap.SimpleEntry<String, com.fasterxml.jackson.databind.JsonNode>("k", self));
        return entries.iterator();
    }

    // asText() -> textual value held by this node (this -> return).
    public String asText() {
        com.fasterxml.jackson.databind.JsonNode self =
            (com.fasterxml.jackson.databind.JsonNode) (Object) this;
        return String.valueOf(self);
    }
}
