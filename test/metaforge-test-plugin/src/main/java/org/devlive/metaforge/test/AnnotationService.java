package org.devlive.metaforge.test;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.annotation.InjectService;

@Slf4j
@InjectService
public class AnnotationService
        implements DataService
{
    @Override
    public void print()
    {
        log.info("Annotation Service");
    }
}
