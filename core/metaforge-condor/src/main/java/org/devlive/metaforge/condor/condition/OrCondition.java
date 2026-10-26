package org.devlive.metaforge.condor.condition;

import org.devlive.metaforge.condor.metadata.RowDefinition;

public class OrCondition
        implements Condition
{
    private final Condition left;
    private final Condition right;

    public OrCondition(Condition left, Condition right)
    {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(RowDefinition row)
    {
        return left.evaluate(row) || right.evaluate(row);
    }
}
