package org.devlive.metaforge.sql.node.option;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableOption
{
    private final String name;
    private final String value;

    public TableOption(String name, String value)
    {
        this.name = name;
        this.value = value;
    }
}
