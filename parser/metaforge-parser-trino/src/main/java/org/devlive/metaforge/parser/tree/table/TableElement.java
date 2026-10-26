package org.devlive.metaforge.parser.tree.table;

import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.node.NodeLocation;

import java.util.Optional;

public abstract class TableElement
        extends Node
{
    public TableElement(Optional<NodeLocation> location)
    {
        super(location);
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitTableElement(this, context);
    }
}
