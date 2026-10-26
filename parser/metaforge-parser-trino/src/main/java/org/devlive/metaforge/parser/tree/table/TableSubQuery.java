package org.devlive.metaforge.parser.tree.table;

import com.google.common.collect.ImmutableList;
import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.node.NodeLocation;
import org.devlive.metaforge.parser.tree.query.Query;
import org.devlive.metaforge.parser.tree.query.QueryBody;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

public class TableSubQuery
        extends QueryBody
{
    private final Query query;

    public TableSubQuery(Query query)
    {
        this(Optional.empty(), query);
    }

    public TableSubQuery(NodeLocation location, Query query)
    {
        this(Optional.of(location), query);
    }

    private TableSubQuery(Optional<NodeLocation> location, Query query)
    {
        super(location);
        this.query = query;
    }

    public Query getQuery()
    {
        return query;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitTableSubquery(this, context);
    }

    @Override
    public List<Node> getChildren()
    {
        return ImmutableList.of(query);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(query)
                .toString();
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
        TableSubQuery tableSubquery = (TableSubQuery) o;
        return Objects.equals(query, tableSubquery.query);
    }

    @Override
    public int hashCode()
    {
        return query.hashCode();
    }

    @Override
    public boolean shallowEquals(Node other)
    {
        return sameClass(this, other);
    }
}
