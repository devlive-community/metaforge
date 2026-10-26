package org.devlive.metaforge.sql.node.clause;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.element.TableElement;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class JoinClause
{
    private JoinType joinType;
    private TableElement rightTable;
    private Expression condition;

    public enum JoinType
    {
        INNER, LEFT, RIGHT, FULL, CROSS, NATURAL
    }
}
