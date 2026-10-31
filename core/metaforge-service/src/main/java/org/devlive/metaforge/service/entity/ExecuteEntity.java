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
    // Optional editor tab label carried into the query history for display.
    private String label;
    private String content;
    private Map<String, Object> env;
    private String format = "JsonConvert";
    private int limit = 1000;
    private QueryMode mode;
    // Table scope from a table's workbench: the statement may only use this table, and runs against this database.
    private String database;
    private String table;
}
