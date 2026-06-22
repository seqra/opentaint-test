package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;

import java.util.List;

/**
 * Container/element taint modeling for io.vertx.core.json.JsonArray.
 *
 * The array stores its elements; tainted values added via the constructor or
 * #add are held in {@link #elem} and read back out through the index accessors
 * (getString / getValue), mirroring the real backing-list behaviour.
 */
@Approximate(io.vertx.core.json.JsonArray.class)
public class JsonArray {

    // Stand-in for the backing list element store.
    private Object elem;

    // JsonArray(List): elements of the source list (arg0) populate the container.
    public JsonArray(List list) {
        if (list != null && !list.isEmpty()) {
            this.elem = list.get(0);
        }
    }

    // add(Object): the added value (arg0) flows into the container; returns this.
    public io.vertx.core.json.JsonArray add(Object value) {
        this.elem = value;
        return (io.vertx.core.json.JsonArray) (Object) this;
    }

    // add(String): typed overload; the added value (arg0) flows into the container.
    public io.vertx.core.json.JsonArray add(String value) {
        this.elem = value;
        return (io.vertx.core.json.JsonArray) (Object) this;
    }

    // Read-back accessors: pull the stored (possibly tainted) element back out.
    public String getString(int index) {
        return (String) this.elem;
    }

    public Object getValue(int index) {
        return this.elem;
    }
}
