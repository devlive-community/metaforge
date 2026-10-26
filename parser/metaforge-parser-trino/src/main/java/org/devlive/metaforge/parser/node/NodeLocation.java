package org.devlive.metaforge.parser.node;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public final class NodeLocation
{
    private final int line;
    private final int charPositionInLine;

    public NodeLocation(int line, int charPositionInLine)
    {
        checkArgument(line >= 1, "line must be at least one, got: %s", line);
        checkArgument(charPositionInLine >= 0, "charPositionInLine must be at least zero, got: %s", charPositionInLine);
        this.line = line;
        this.charPositionInLine = charPositionInLine;
    }

    public int getLineNumber()
    {
        return line;
    }

    public int getColumnNumber()
    {
        return charPositionInLine + 1;
    }

    @Override
    public String toString()
    {
        return line + ":" + charPositionInLine;
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
        NodeLocation that = (NodeLocation) o;
        return line == that.line &&
                charPositionInLine == that.charPositionInLine;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(line, charPositionInLine);
    }
}
