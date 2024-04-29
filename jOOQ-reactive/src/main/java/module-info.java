/**
 * The jOOQ reactive module.
 */
module org.jooq.reactive {

    // Other jOOQ modules
    requires transitive org.jooq;

    // Nullability annotations for better Kotlin interop
    requires static org.jetbrains.annotations;
    requires org.reactivestreams;
    requires reactor.core;

    exports org.jooq.reactive;


}