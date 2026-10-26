package org.devlive.metaforge.spi.generator;

public class OrderBy
{
    private final String column;
    private final Direction direction;

    public enum Direction
    {
        asc,
        desc;

        @Override
        public String toString()
        {
            return name();
        }
    }

    private OrderBy(String column, Direction direction)
    {
        this.column = column;
        this.direction = direction;
    }

    public static OrderBy create(String column)
    {
        return new OrderBy(column, Direction.asc);
    }

    public static OrderBy create(String column, Direction direction)
    {
        return new OrderBy(column, direction);
    }

    public String build()
    {
        return String.format("`%s` %s", column, direction);
    }
}
