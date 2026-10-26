package org.devlive.metaforge.plugin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标注Service实现类的注解
 * Annotation for marking Service implementation classes
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectService
{
    /**
     * 指定要实现的服务接口
     * Specify the service interface to implement
     */
    Class<?>[] value() default {};
}
