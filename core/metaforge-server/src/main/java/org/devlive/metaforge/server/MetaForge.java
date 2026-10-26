package org.devlive.metaforge.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

import java.io.File;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, SolrAutoConfiguration.class})
@ComponentScans(value = {
        @ComponentScan(value = "org.devlive.metaforge.security"),
        @ComponentScan(value = "org.devlive.metaforge.service"),
        @ComponentScan(value = "org.devlive.metaforge.schedule"),
        @ComponentScan(value = "org.devlive.metaforge.scheduler")
})
public class MetaForge
{
    public void start(String[] args)
    {
        SpringApplication.run(MetaForge.class, args);
    }

    public static void main(String[] args)
    {
        String logbackConfigure = System.getProperty("user.dir") + "/configure/logback.xml";
        if (new File(logbackConfigure).exists()) {
            System.setProperty("logging.config", logbackConfigure);
        }
        new MetaForge().start(args);
    }
}
