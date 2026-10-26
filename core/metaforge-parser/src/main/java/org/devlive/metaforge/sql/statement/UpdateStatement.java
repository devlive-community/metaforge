package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.devlive.metaforge.sql.node.Expression;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UpdateStatement
        extends SQLStatement
{
    private final String tableName;
    private final Map<String, Expression> setValues;
    private final Expression whereClause;

    public UpdateStatement(String tableName, Map<String, Expression> setValues, Expression whereClause)
    {
        super(StatementType.UPDATE);
        this.tableName = tableName;
        this.setValues = setValues;
        this.whereClause = whereClause;
    }
}
