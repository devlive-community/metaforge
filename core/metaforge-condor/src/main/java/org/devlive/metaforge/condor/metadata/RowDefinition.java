package org.devlive.metaforge.condor.metadata;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RowDefinition
{
    @Getter
    @Setter
    private Map<String, Object> values;

    public RowDefinition()
    {
        this.values = new HashMap<>();
    }

    public void setValue(String columnName, Object value)
    {
        values.put(columnName, value);
    }

    public Object getValue(String columnName)
    {
        return values.get(columnName);
    }
}
