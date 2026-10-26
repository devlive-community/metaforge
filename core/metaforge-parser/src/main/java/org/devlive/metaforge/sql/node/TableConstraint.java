package org.devlive.metaforge.sql.node;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.devlive.metaforge.sql.node.clause.ForeignKeyClause;
import org.devlive.metaforge.sql.node.element.TableElement;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableConstraint
        extends TableElement
{
    private final String constraintName;
    private final ConstraintType type;
    private final String[] columns;
    private final ForeignKeyClause foreignKey;
    private final Expression checkExpression;

    public TableConstraint(String constraintName, ConstraintType type, String[] columns,
            ForeignKeyClause foreignKey, Expression checkExpression)
    {
        this.constraintName = constraintName;
        this.type = type;
        this.columns = columns;
        this.foreignKey = foreignKey;
        this.checkExpression = checkExpression;
    }
}
