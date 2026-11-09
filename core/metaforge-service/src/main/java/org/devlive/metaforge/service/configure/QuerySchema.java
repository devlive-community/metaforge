package org.devlive.metaforge.service.configure;

import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType;

import java.util.List;

/**
 * 查询页执行 SQL 的可配置字段 schema。
 *
 * Field schema for statements run from the query page (category QUERY, name Default).
 */
public final class QuerySchema
{
    private QuerySchema() {}

    public static final String NAME = "Default";
    public static final String MAX_ROWS = "maxRows";
    public static final int DEFAULT_MAX_ROWS = 1000;
    /** Upper bound an administrator can set: the whole result travels to the browser as one response. */
    public static final int LIMIT_MAX_ROWS = 100_000;

    public static List<RuntimeConfigureField> fields()
    {
        return List.of(
                new RuntimeConfigureField(MAX_ROWS, RuntimeConfigureFieldType.NUMBER, String.valueOf(DEFAULT_MAX_ROWS),
                        "Most rows a query returns (1 to " + LIMIT_MAX_ROWS + "); longer results are cut and marked as truncated", false)
        );
    }

    /** The configured cap, or the default when the value is missing or out of range. */
    public static int maxRows(String value)
    {
        Integer parsed = parse(value);
        return parsed == null ? DEFAULT_MAX_ROWS : parsed;
    }

    /** Null when the value is not a whole number from 1 to {@link #LIMIT_MAX_ROWS}. */
    public static Integer parse(String value)
    {
        try {
            int parsed = Integer.parseInt(String.valueOf(value).trim());
            return parsed >= 1 && parsed <= LIMIT_MAX_ROWS ? parsed : null;
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }
}
