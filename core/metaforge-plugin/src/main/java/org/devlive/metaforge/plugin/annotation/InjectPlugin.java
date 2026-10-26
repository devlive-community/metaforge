package org.devlive.metaforge.plugin.annotation;

import org.devlive.metaforge.plugin.PluginType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectPlugin
{
    /**
     * 插件名称
     * Plugin name
     */
    String name() default "";

    /**
     * 插件版本
     * Plugin version
     */
    String version() default "1.0.0";

    PluginType type() default PluginType.CONNECTOR;
}
