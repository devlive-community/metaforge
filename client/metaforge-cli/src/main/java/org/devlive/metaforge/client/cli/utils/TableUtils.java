package org.devlive.metaforge.client.cli.utils;

import org.springframework.shell.table.BorderSpecification;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.SimpleHorizontalAligner;
import org.springframework.shell.table.SimpleVerticalAligner;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.Tables;

public class TableUtils
{
    private TableUtils()
    {}

    /**
     * Customize the given TableBuilder with the following common features (these choices can
     * always be overridden by applying later customizations) :
     * <ul>
     * <li>double border around the whole table and first row</li>
     * <li>vertical space (air) borders, single line separators between rows</li>
     * <li>first row is assumed to be a header and is centered horizontally and
     * vertically</li>
     * <li>cells containing Map values are rendered as {@literal key = value} lines, trying to
     * align on equal signs</li>
     * </ul>
     *
     * @param builder the table builder to use
     * @return the configured table builder
     */
    public static TableBuilder applyStyle(TableBuilder builder)
    {
        builder.addOutlineBorder(BorderStyle.fancy_double)
                .paintBorder(BorderStyle.air, BorderSpecification.INNER_VERTICAL).fromTopLeft().toBottomRight()
                .paintBorder(BorderStyle.fancy_light, BorderSpecification.INNER_VERTICAL).fromTopLeft()
                .toBottomRight()
                .addHeaderBorder(BorderStyle.fancy_double).on(CellMatchers.row(0))
                .addAligner(SimpleVerticalAligner.middle).addAligner(SimpleHorizontalAligner.center);
        return Tables.configureKeyValueRendering(builder, " = ");
    }

    /**
     * Customize the given TableBuilder with almost same way than
     * {@link #applyStyle(TableBuilder)} but do not use any header styling.
     *
     * @param builder the table builder to use
     * @return the configured table builder
     */
    public static TableBuilder applyStyleNoHeader(TableBuilder builder)
    {
        builder.addOutlineBorder(BorderStyle.fancy_double)
                .paintBorder(BorderStyle.air, BorderSpecification.INNER_VERTICAL).fromTopLeft().toBottomRight()
                .paintBorder(BorderStyle.fancy_light, BorderSpecification.INNER_VERTICAL).fromTopLeft()
                .toBottomRight();
        return Tables.configureKeyValueRendering(builder, " = ");
    }
}
