package org.devlive.metaforge.sql.node.element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.devlive.metaforge.sql.node.clause.JoinClause;
import org.devlive.metaforge.sql.statement.SelectStatement;

import java.util.List;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableElement
{
    private String tableName;
    private String alias;
    private List<JoinClause> joins;
    private SelectStatement subquery;
}
