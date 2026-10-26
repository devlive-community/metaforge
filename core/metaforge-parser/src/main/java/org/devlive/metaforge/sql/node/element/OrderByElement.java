package org.devlive.metaforge.sql.node.element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.devlive.metaforge.sql.node.Expression;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class OrderByElement
{
    private Expression expression;
    private boolean ascending;
}
