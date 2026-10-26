package org.devlive.metaforge.parser.tree.join;

import com.google.common.collect.ImmutableList;
import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.node.NodeLocation;
import org.devlive.metaforge.parser.tree.Relation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class Join
        extends Relation
{
    private final Type type;
    private final Relation left;
    private final Relation right;
    private final Optional<JoinCriteria> criteria;

    public Join(Type type, Relation left, Relation right, Optional<JoinCriteria> criteria)
    {
        this(Optional.empty(), type, left, right, criteria);
    }

    public Join(NodeLocation location, Type type, Relation left, Relation right, Optional<JoinCriteria> criteria)
    {
        this(Optional.of(location), type, left, right, criteria);
    }

    private Join(Optional<NodeLocation> location, Type type, Relation left, Relation right, Optional<JoinCriteria> criteria)
    {
        super(location);
        requireNonNull(left, "left is null");
        requireNonNull(right, "right is null");
        if ((type == Type.CROSS) || (type == Type.IMPLICIT)) {
            checkArgument(!criteria.isPresent(), "%s join cannot have join criteria", type);
        }
        else {
            checkArgument(criteria.isPresent(), "No join criteria specified");
        }

        this.type = type;
        this.left = left;
        this.right = right;
        this.criteria = criteria;
    }

    public Type getType()
    {
        return type;
    }

    public Relation getLeft()
    {
        return left;
    }

    public Relation getRight()
    {
        return right;
    }

    public Optional<JoinCriteria> getCriteria()
    {
        return criteria;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitJoin(this, context);
    }

    @Override
    public List<Node> getChildren()
    {
        ImmutableList.Builder<Node> nodes = ImmutableList.builder();
        nodes.add(left);
        nodes.add(right);
        criteria.map(JoinCriteria::getNodes)
                .ifPresent(nodes::addAll);
        return nodes.build();
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("type", type)
                .add("left", left)
                .add("right", right)
                .add("criteria", criteria)
                .omitNullValues()
                .toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        Join join = (Join) o;
        return (type == join.type) &&
                Objects.equals(left, join.left) &&
                Objects.equals(right, join.right) &&
                Objects.equals(criteria, join.criteria);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, left, right, criteria);
    }

    @Override
    public boolean shallowEquals(Node other)
    {
        if (!sameClass(this, other)) {
            return false;
        }

        return type.equals(((Join) other).type);
    }

    public enum Type
    {
        CROSS, INNER, LEFT, RIGHT, FULL, IMPLICIT
    }
}
