package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.ToString;
import org.devlive.metaforge.service.configure.IConfigure;

@Data
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class PluginEntity
{
    private String name;
    private String description;
    private String type;
    private IConfigure configure;
}
