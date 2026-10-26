package org.devlive.metaforge.parser.tree.join;

import com.google.common.collect.ImmutableList;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.tree.Identifier;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class JoinUsing
        extends JoinCriteria
{
    private final List<Identifier> columns;

    public JoinUsing(List<Identifier> columns)
    {
        requireNonNull(columns, "columns is null");
        checkArgument(!columns.isEmpty(), "columns is empty");
        this.columns = ImmutableList.copyOf(columns);
    }

    public List<Identifier> getColumns()
    {
        return columns;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        JoinUsing o = (JoinUsing) obj;
        return Objects.equals(columns, o.columns);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(columns);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(columns)
                .toString();
    }

    @Override
    public List<Node> getNodes()
    {
        return ImmutableList.of();
    }
}
