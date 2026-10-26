package org.devlive.metaforge.condor;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SQLResult<T>
{
    private final boolean success;
    private final String message;
    private T data;

    public SQLResult(boolean success, String message)
    {
        this.success = success;
        this.message = message;
    }

    public SQLResult(boolean success, String message, T data)
    {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}
