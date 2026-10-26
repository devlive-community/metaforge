package org.devlive.metaforge.common.sql.configure;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SqlBody
{
    private String database;
    private String table;
    private List<SqlColumn> columns;
    private List<SqlColumn> groups;
    private List<SqlColumn> orders;
    private Integer limit;
    private Integer offset;
    private List<SqlColumn> where;
    private SqlType type;
    // Currently, only autoincrement columns can be reset
    private Object value;
    private String condition;
    private boolean preview;
}
