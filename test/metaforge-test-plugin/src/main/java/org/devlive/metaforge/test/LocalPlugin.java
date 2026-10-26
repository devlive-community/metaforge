package org.devlive.metaforge.test;

import com.google.inject.name.Names;
import org.devlive.metaforge.plugin.Plugin;

public class LocalPlugin
        extends Plugin
{
    @Override
    protected void configurePlug()
    {
        bind(String.class).annotatedWith(Names.named("host")).toInstance("localhost");
    }
}
