package org.devlive.metaforge.parser.tree.query;

import com.google.common.collect.ImmutableList;
import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.node.NodeLocation;
import org.devlive.metaforge.parser.tree.FetchFirst;
import org.devlive.metaforge.parser.tree.Limit;
import org.devlive.metaforge.parser.tree.Offset;
import org.devlive.metaforge.parser.tree.OrderBy;
import org.devlive.metaforge.parser.tree.Statement;
import org.devlive.metaforge.parser.tree.With;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class Query
        extends Statement
{
    private final Optional<With> with;
    private final QueryBody queryBody;
    private final Optional<OrderBy> orderBy;
    private final Optional<Offset> offset;
    private final Optional<Node> limit;

    public Query(
            Optional<With> with,
            QueryBody queryBody,
            Optional<OrderBy> orderBy,
            Optional<Offset> offset,
            Optional<Node> limit)
    {
        this(Optional.empty(), with, queryBody, orderBy, offset, limit);
    }

    public Query(
            NodeLocation location,
            Optional<With> with,
            QueryBody queryBody,
            Optional<OrderBy> orderBy,
            Optional<Offset> offset,
            Optional<Node> limit)
    {
        this(Optional.of(location), with, queryBody, orderBy, offset, limit);
    }

    private Query(
            Optional<NodeLocation> location,
            Optional<With> with,
            QueryBody queryBody,
            Optional<OrderBy> orderBy,
            Optional<Offset> offset,
            Optional<Node> limit)
    {
        super(location);
        requireNonNull(with, "with is null");
        requireNonNull(queryBody, "queryBody is null");
        requireNonNull(orderBy, "orderBy is null");
        requireNonNull(offset, "offset is null");
        requireNonNull(limit, "limit is null");
        checkArgument(!limit.isPresent() || limit.get() instanceof FetchFirst || limit.get() instanceof Limit, "limit must be optional of either FetchFirst or Limit type");
        this.with = with;
        this.queryBody = queryBody;
        this.orderBy = orderBy;
        this.offset = offset;
        this.limit = limit;
    }

    public Optional<With> getWith()
    {
        return with;
    }

    public QueryBody getQueryBody()
    {
        return queryBody;
    }

    public Optional<OrderBy> getOrderBy()
    {
        return orderBy;
    }

    public Optional<Offset> getOffset()
    {
        return offset;
    }

    public Optional<Node> getLimit()
    {
        return limit;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitQuery(this, context);
    }

    @Override
    public List<Node> getChildren()
    {
        ImmutableList.Builder<Node> nodes = ImmutableList.builder();
        with.ifPresent(nodes::add);
        nodes.add(queryBody);
        orderBy.ifPresent(nodes::add);
        offset.ifPresent(nodes::add);
        limit.ifPresent(nodes::add);
        return nodes.build();
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("with", with.orElse(null))
                .add("queryBody", queryBody)
                .add("orderBy", orderBy)
                .add("offset", offset.orElse(null))
                .add("limit", limit.orElse(null))
                .omitNullValues()
                .toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Query o = (Query) obj;
        return Objects.equals(with, o.with) &&
                Objects.equals(queryBody, o.queryBody) &&
                Objects.equals(orderBy, o.orderBy) &&
                Objects.equals(offset, o.offset) &&
                Objects.equals(limit, o.limit);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(with, queryBody, orderBy, offset, limit);
    }

    @Override
    public boolean shallowEquals(Node other)
    {
        return sameClass(this, other);
    }
}
