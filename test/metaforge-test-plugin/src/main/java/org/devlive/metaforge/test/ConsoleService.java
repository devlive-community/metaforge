package org.devlive.metaforge.test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleService
        implements DataService
{
    @Override
    public void print()
    {
        log.info("Console Service");
    }
}
