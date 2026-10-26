package org.devlive.metaforge.sql.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.clause.LimitClause;
import org.devlive.metaforge.sql.node.element.OrderByElement;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;

import java.util.List;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SelectStatement
        extends SQLStatement
{
    private List<SelectElement> selectElements;
    private List<TableElement> fromSources;
    private Expression whereClause;
    private List<Expression> groupByElements;
    private Expression havingClause;
    private List<OrderByElement> orderByElements;
    private LimitClause limitClause;

    public SelectStatement()
    {
        super(StatementType.SELECT);
    }
}
