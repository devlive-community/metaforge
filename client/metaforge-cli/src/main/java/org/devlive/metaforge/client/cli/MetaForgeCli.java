package org.devlive.metaforge.client.cli;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MetaForgeCli
{
    public void start(String[] args)
    {
        SpringApplication application = new SpringApplication(MetaForgeCli.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);
        application.run(args);
    }

    public static void main(String[] args)
    {
        new MetaForgeCli().start(args);
    }
}
