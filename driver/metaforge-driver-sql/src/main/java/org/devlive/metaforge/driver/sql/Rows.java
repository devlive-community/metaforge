package org.devlive.metaforge.driver.sql;

import java.util.ArrayList;
import java.util.List;

/** A statement's result: labelled, typed rows, or the number of rows changed. */
public final class Rows
{
    private final List<String> labels;
    private final List<String> types;
    private final List<List<Object>> values;
    private final int updateCount;

    private Rows(List<String> labels, List<String> types, List<List<Object>> values, int updateCount)
    {
        this.labels = List.copyOf(labels);
        this.types = List.copyOf(types);
        this.values = new ArrayList<>(values);
        this.updateCount = updateCount;
    }

    public static Rows of(List<String> labels, List<String> types, List<List<Object>> values)
    {
        return new Rows(labels, types, values, -1);
    }

    public static Rows updated(int count)
    {
        return new Rows(List.of(), List.of(), List.of(), count);
    }

    public boolean isQuery()
    {
        return updateCount < 0;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public List<String> getTypes()
    {
        return types;
    }

    public List<List<Object>> getValues()
    {
        return new ArrayList<>(values);
    }

    public int getUpdateCount()
    {
        return updateCount;
    }
}
