package org.devlive.metaforge.sql.node;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class DataType
{
    private final String baseType;
    private final Integer[] parameters;  // For types like VARCHAR(255)

    public DataType(String baseType, Integer[] parameters)
    {
        this.baseType = baseType;
        this.parameters = parameters;
    }
}