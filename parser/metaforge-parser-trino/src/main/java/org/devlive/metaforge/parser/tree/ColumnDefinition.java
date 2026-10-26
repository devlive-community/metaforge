package org.devlive.metaforge.parser.tree;

import com.google.common.collect.ImmutableList;
import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.node.NodeLocation;
import org.devlive.metaforge.parser.tree.table.TableElement;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public final class ColumnDefinition
        extends TableElement
{
    private final Identifier name;
    private final DataType type;
    private final boolean nullable;
    private final List<Property> properties;
    private final Optional<String> comment;

    public ColumnDefinition(Identifier name, DataType type, boolean nullable, List<Property> properties, Optional<String> comment)
    {
        this(Optional.empty(), name, type, nullable, properties, comment);
    }

    public ColumnDefinition(NodeLocation location, Identifier name, DataType type, boolean nullable, List<Property> properties, Optional<String> comment)
    {
        this(Optional.of(location), name, type, nullable, properties, comment);
    }

    private ColumnDefinition(Optional<NodeLocation> location, Identifier name, DataType type, boolean nullable, List<Property> properties, Optional<String> comment)
    {
        super(location);
        this.name = requireNonNull(name, "name is null");
        this.type = requireNonNull(type, "type is null");
        this.nullable = nullable;
        this.properties = requireNonNull(properties, "properties is null");
        this.comment = requireNonNull(comment, "comment is null");
    }

    public Identifier getName()
    {
        return name;
    }

    public DataType getType()
    {
        return type;
    }

    public boolean isNullable()
    {
        return nullable;
    }

    public List<Property> getProperties()
    {
        return properties;
    }

    public Optional<String> getComment()
    {
        return comment;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitColumnDefinition(this, context);
    }

    @Override
    public List<Node> getChildren()
    {
        return ImmutableList.of();
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
        ColumnDefinition o = (ColumnDefinition) obj;
        return Objects.equals(this.name, o.name) &&
                Objects.equals(this.type, o.type) &&
                this.nullable == o.nullable &&
                Objects.equals(properties, o.properties) &&
                Objects.equals(this.comment, o.comment);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, type, properties, comment, nullable);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("nullable", nullable)
                .add("properties", properties)
                .add("comment", comment)
                .toString();
    }
}
