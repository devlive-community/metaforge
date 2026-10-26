package org.devlive.metaforge.service.initializer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "metaforge.fs")
public class FsConfigure
{
    private String type;
    private String access;
    private String secret;
    private String endpoint;
    private String bucket;
}
