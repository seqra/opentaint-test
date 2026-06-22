package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;

/**
 * Container/element taint modeling for io.vertx.core.json.JsonObject.
 *
 * #mapTo binds the json container's fields (held on this) onto a new typed
 * bean, so taint on the container flows onto the returned mapped object
 * (coarse this -> result propagation, matching the reflective bean binding).
 */
@Approximate(io.vertx.core.json.JsonObject.class)
public class JsonObject {

    // mapTo(Class): the container's tainted fields (this) flow onto the mapped bean.
    public Object mapTo(Class clazz) {
        io.vertx.core.json.JsonObject self = (io.vertx.core.json.JsonObject) (Object) this;
        return self;
    }
}
