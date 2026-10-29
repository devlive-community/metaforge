package org.devlive.metaforge.service.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a transient field that holds the entity referenced by an id column of the same entity.
 * Nothing is fetched automatically: {@link References#load} fills the field on demand in one
 * query per reference, and saving copies the referenced entity's id into the id column.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference
{
    /**
     * Name of the field that stores the referenced id.
     */
    String value();
}
