package com.example.approximations.com_jayway_jsonpath;

import org.opentaint.ir.approximation.annotation.Approximate;

/**
 * Dataflow approximation for com.jayway.jsonpath.ReadContext.
 *
 * A ReadContext wraps a parsed JSON document (obtained via JsonPath.parse(...),
 * whose taint flows into the context). The read(...) overloads evaluate a path
 * expression against that document and return the selected fragment, so taint on
 * the underlying document carried by the context (this) flows to the read result.
 *
 * Each read overload is modelled as this -> result: it returns the context
 * object itself, which carries the document's taint. The erased return type of
 * every overload is the type variable T, i.e. java.lang.Object, so a downstream
 * consumer of the fragment sees the propagated taint.
 *
 * The Predicate... / TypeRef / Class / JsonPath arguments only select or shape the
 * fragment; they do not introduce or remove taint, so they are not copied.
 */
@Approximate(com.jayway.jsonpath.ReadContext.class)
public class ReadContext {

    // read(String path, Predicate... filters) -> selected fragment of the document.
    public Object read(String path, com.jayway.jsonpath.Predicate... filters) {
        return (Object) this;
    }

    // read(String path, Class<T> type, Predicate... filters) -> typed fragment.
    public Object read(String path, Class type, com.jayway.jsonpath.Predicate... filters) {
        return (Object) this;
    }

    // read(String path, TypeRef<T> type) -> generically-typed fragment.
    public Object read(String path, com.jayway.jsonpath.TypeRef type) {
        return (Object) this;
    }

    // read(JsonPath path) -> selected fragment of the document.
    public Object read(com.jayway.jsonpath.JsonPath path) {
        return (Object) this;
    }

    // read(JsonPath path, Class<T> type) -> typed fragment.
    public Object read(com.jayway.jsonpath.JsonPath path, Class type) {
        return (Object) this;
    }

    // read(JsonPath path, TypeRef<T> type) -> generically-typed fragment.
    public Object read(com.jayway.jsonpath.JsonPath path, com.jayway.jsonpath.TypeRef type) {
        return (Object) this;
    }
}
