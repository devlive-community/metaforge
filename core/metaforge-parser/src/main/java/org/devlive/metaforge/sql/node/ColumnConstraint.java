package org.devlive.metaforge.sql.node;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.devlive.metaforge.sql.node.clause.ForeignKeyClause;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ColumnConstraint
{
    private final String constraintName;
    private final ConstraintType type;
    private final Object value;  // For DEFAULT value
    private final ForeignKeyClause foreignKey;
    private final Expression checkExpression;

    public ColumnConstraint(String constraintName, ConstraintType type, Object value,
            ForeignKeyClause foreignKey, Expression checkExpression)
    {
        this.constraintName = constraintName;
        this.type = type;
        this.value = value;
        this.foreignKey = foreignKey;
        this.checkExpression = checkExpression;
    }
}
