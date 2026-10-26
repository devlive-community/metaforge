package org.devlive.metaforge.spi.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Time
{
    @Getter
    @Setter
    private long start;
    @Getter
    private long end;
    @Getter
    private long elapsed;

    public void setEnd(long end)
    {
        this.end = end;
        this.elapsed = end - start;
    }
}
