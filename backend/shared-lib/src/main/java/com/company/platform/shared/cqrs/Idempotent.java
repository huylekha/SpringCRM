package com.company.platform.shared.cqrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for commands that should be processed idempotently. Commands marked with this
 * annotation will have their responses cached and duplicate requests will return the cached
 * response.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

  /** The time-to-live for the idempotency cache in seconds. Default is 1 hour (3600 seconds). */
  long ttlSeconds() default 3600L;
}
