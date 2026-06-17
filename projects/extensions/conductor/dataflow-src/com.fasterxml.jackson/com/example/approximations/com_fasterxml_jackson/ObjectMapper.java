package com.example.approximations.com_fasterxml_jackson;

import org.opentaint.ir.approximation.annotation.Approximate;

import java.util.ArrayList;
import java.util.List;

/**
 * Dataflow approximation for com.fasterxml.jackson.databind.ObjectMapper.
 *
 * Models the binding/deserialization methods whose taint flows through a
 * generic / collection container rather than a flat copy:
 *
 *   - convertValue(Object, TypeReference): the source object is re-bound into
 *     the target generic type; taint on the input object reaches the elements
 *     of the produced container.
 *   - readValue(String, TypeReference): the tainted JSON text is deserialized
 *     into the target generic type (e.g. List<String>); taint on the text
 *     reaches the produced container's elements.
 *   - readTree(String): the tainted JSON text is parsed into a JsonNode tree;
 *     taint reaches the node so it can be pulled back out via the node's
 *     accessors / iterators (fields(), elements(), asText()).
 *
 * The returned containers are built so a downstream extractor (List.get(i),
 * the JsonNode iterators, etc.) can recover the tainted value.
 */
@Approximate(com.fasterxml.jackson.databind.ObjectMapper.class)
public class ObjectMapper {

    // convertValue(Object, TypeReference) -> generic container holding the
    // re-bound input. Erased return type is Object.
    public Object convertValue(Object fromValue,
                               com.fasterxml.jackson.core.type.TypeReference toValueTypeRef) {
        List<Object> container = new ArrayList<Object>();
        container.add(fromValue);
        return container;
    }

    // readValue(String, TypeReference) -> generic container deserialized from
    // the tainted JSON text. Erased return type is Object.
    public Object readValue(String content,
                            com.fasterxml.jackson.core.type.TypeReference valueTypeRef)
            throws java.io.IOException {
        List<Object> container = new ArrayList<Object>();
        container.add(content);
        return container;
    }

    // readTree(String) -> JsonNode tree carrying the tainted text. Re-wrap into
    // a JsonNode whose accessors/iterators surface the taint.
    public com.fasterxml.jackson.databind.JsonNode readTree(String content)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return com.fasterxml.jackson.databind.node.TextNode.valueOf(content);
    }
}
