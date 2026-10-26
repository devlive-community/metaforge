package org.devlive.metaforge.parser.tree.join;

import org.devlive.metaforge.parser.node.Node;

import java.util.List;

public abstract class JoinCriteria
{
    // Force subclasses to have a proper equals and hashcode implementation
    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract List<Node> getNodes();
}
