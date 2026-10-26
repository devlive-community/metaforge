package org.devlive.metaforge.sql.node.clause;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class LimitClause
{
    private long limit;
    private long offset;
}
