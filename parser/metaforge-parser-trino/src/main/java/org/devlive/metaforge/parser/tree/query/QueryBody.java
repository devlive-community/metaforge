package org.devlive.metaforge.parser.tree.query;

import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.NodeLocation;
import org.devlive.metaforge.parser.tree.Relation;

import java.util.Optional;

public abstract class QueryBody
        extends Relation
{
    protected QueryBody(Optional<NodeLocation> location)
    {
        super(location);
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitQueryBody(this, context);
    }
}
