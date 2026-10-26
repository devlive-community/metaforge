package org.devlive.metaforge.sql.node;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class Expression
{
    private ExpressionType type;
    private Object value;
    private List<Expression> children;

    public enum ExpressionType
    {
        LITERAL, COLUMN_REFERENCE, FUNCTION_CALL, BINARY_OP, UNARY_OP, FUNCTION
    }
}
