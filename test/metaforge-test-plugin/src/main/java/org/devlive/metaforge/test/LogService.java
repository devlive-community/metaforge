package org.devlive.metaforge.test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogService
        implements DataService
{
    @Override
    public void print()
    {
        log.info("Log Service");
    }
}
