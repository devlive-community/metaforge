package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.devlive.metaforge.sql.node.Expression;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ShowStatement
        extends SQLStatement
{
    private ShowType showType;
    private String databaseName;
    private String tableName;
    private String pattern;
    private Expression whereCondition;

    public ShowStatement()
    {
        super(StatementType.SHOW);
    }

    public enum ShowType
    {
        DATABASES,
        TABLES,
        COLUMNS
    }
}
