package org.devlive.metaforge.service.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.ToString;
import org.devlive.metaforge.service.enums.QueryMode;

import java.util.Map;

@Data
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ExecuteEntity
{
    private String name;
    private String content;
    private Map<String, Object> env;
    private String format = "JsonConvert";
    private int limit = 1000;
    private QueryMode mode;
}
