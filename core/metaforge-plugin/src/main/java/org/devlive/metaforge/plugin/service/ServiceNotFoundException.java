package org.devlive.metaforge.plugin.service;

public class ServiceNotFoundException
        extends RuntimeException
{
    public ServiceNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
