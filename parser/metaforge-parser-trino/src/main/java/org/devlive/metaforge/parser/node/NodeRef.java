package org.devlive.metaforge.parser.node;

import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static java.util.Objects.requireNonNull;

public final class NodeRef<T extends Node>
{
    private final T node;

    private NodeRef(T node)
    {
        this.node = requireNonNull(node, "node is null");
    }

    public static <T extends Node> NodeRef<T> of(T node)
    {
        return new NodeRef<>(node);
    }

    public T getNode()
    {
        return node;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeRef<?> other = (NodeRef<?>) o;
        return node == other.node;
    }

    @Override
    public int hashCode()
    {
        return identityHashCode(node);
    }

    @Override
    public String toString()
    {
        return format(
                "@%s: %s",
                Integer.toHexString(identityHashCode(node)),
                node);
    }
}
