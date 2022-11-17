package net.sumaris.core.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author peck7 on 15/09/2020.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.FIELD})
public @interface Comment {

    /**
     * Return the comment applied on a table or a column
     *
     * @return the comment
     */
    String value();
}
