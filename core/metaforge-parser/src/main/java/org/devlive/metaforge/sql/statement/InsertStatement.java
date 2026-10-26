package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.devlive.metaforge.sql.node.Expression;

import java.util.List;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class InsertStatement
        extends SQLStatement
{
    private final String tableName;
    private final boolean orReplace;
    private final List<String> columns;
    private final List<List<Expression>> values;
    private final List<List<Object>> simpleValues;
    private final SelectStatement select;

    public InsertStatement(
            String tableName,
            boolean orReplace,
            List<String> columns,
            List<List<Expression>> values,
            List<List<Object>> simpleValues,
            SelectStatement select
    )
    {
        super(StatementType.INSERT);
        this.tableName = tableName;
        this.orReplace = orReplace;
        this.columns = columns;
        this.values = values;
        this.simpleValues = simpleValues;
        this.select = select;
    }
}
