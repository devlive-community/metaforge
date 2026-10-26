package org.devlive.metaforge.sql.node.element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.devlive.metaforge.sql.node.Expression;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SelectElement
{
    private String column;
    private String alias;
    private Expression expression;
}
