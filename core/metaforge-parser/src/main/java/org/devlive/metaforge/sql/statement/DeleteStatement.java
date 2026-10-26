package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.devlive.metaforge.sql.node.Expression;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class DeleteStatement
        extends SQLStatement
{
    private final String tableName;
    private final Expression whereClause;

    public DeleteStatement(String tableName, Expression whereClause)
    {
        super(StatementType.DELETE);
        this.tableName = tableName;
        this.whereClause = whereClause;
    }
}
