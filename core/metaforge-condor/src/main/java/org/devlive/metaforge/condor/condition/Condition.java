package org.devlive.metaforge.condor.condition;

import org.devlive.metaforge.condor.metadata.RowDefinition;

public interface Condition
{
    boolean evaluate(RowDefinition row);
}
