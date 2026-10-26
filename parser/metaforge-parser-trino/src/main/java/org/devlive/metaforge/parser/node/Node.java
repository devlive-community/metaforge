package org.devlive.metaforge.parser.node;

import org.devlive.metaforge.parser.ast.AstVisitor;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public abstract class Node
{
    private final Optional<NodeLocation> location;

    protected Node(Optional<NodeLocation> location)
    {
        this.location = requireNonNull(location, "location is null");
    }

    protected static boolean sameClass(Node left, Node right)
    {
        if (left == right) {
            return true;
        }
        return left.getClass() == right.getClass();
    }

    /**
     * Accessible for {@link AstVisitor}, use {@link AstVisitor#process(Node, Object)} instead.
     */
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitNode(this, context);
    }

    public Optional<NodeLocation> getLocation()
    {
        return location;
    }

    public abstract List<? extends Node> getChildren();

    // Force subclasses to have a proper equals and hashcode implementation
    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();

    /**
     * Compare with another node by considering internal state excluding any Node returned by getChildren()
     */
    public boolean shallowEquals(Node other)
    {
        throw new UnsupportedOperationException("not yet implemented: " + getClass().getName());
    }
}
