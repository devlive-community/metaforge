package org.devlive.metaforge.common.sql.configure;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Properties;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SqlColumn
{
    private String column;
    private Object value;
    private String expression;
    private String alias;
    private SqlOperator operator;
    private SqlOrder order;
    private Properties original;
}
