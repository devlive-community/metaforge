package org.devlive.metaforge.service.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditPlugin
{
    String plugin() default "";

    public boolean isRequest() default true;

    public boolean isResponse() default false;
}
