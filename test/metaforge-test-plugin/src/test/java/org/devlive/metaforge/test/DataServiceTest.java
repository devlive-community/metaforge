package org.devlive.metaforge.test;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.PluginConfigure;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.utils.PluginPathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Set;

@Slf4j
public class DataServiceTest
{
    private PluginManager pluginManager;

    @Before
    public void before()
    {
        Path projectRoot = PluginPathUtils.findProjectRoot();
        PluginConfigure config = PluginConfigure.builder()
                .pluginsDir(projectRoot.resolve("test/metaforge-test-plugin"))
                .scanDepth(3)
                .build();

        pluginManager = new PluginManager(config);
        pluginManager.start();
    }

    @Test
    public void testLoadPlugin()
    {
        Assert.assertFalse(pluginManager.getPlugin("Local").isEmpty());
    }

    @Test
    public void testService()
    {
        pluginManager.getPlugin("Local")
                .ifPresent(value -> {
                    log.info("Specify the Service class");
                    DataService service = value.getService(ConsoleService.class);
                    service.print();

//                    log.info("Specify the Service qualifier name");
//                    DataService logService = value.getService(DataService.class, "LogService");
//                    logService.print();
//
//                    log.info("Found annotation Service");
//                    AnnotationService annotationService = value.getService(AnnotationService.class);
//                    annotationService.print();
//                    DataService annotationService2 = value.getService(DataService.class, "AnnotationService");
//                    annotationService2.print();

                    log.info("Get all Service");
                    Set<DataService> services = value.getAllServices(DataService.class);
                    services.forEach(DataService::print);
                });
    }
}
