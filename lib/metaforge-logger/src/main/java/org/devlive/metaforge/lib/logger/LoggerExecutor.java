package org.devlive.metaforge.lib.logger;

import org.slf4j.Logger;

public abstract class LoggerExecutor<T>
{
    protected String directory;
    protected String name;

    public LoggerExecutor(final String directory, final String name)
    {
        this.directory = directory;
        this.name = name;
    }

    public abstract Logger getLogger();

    public abstract void destroy();
}
