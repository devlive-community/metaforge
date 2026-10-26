package org.devlive.metaforge.spi.generator.column;

import org.devlive.metaforge.spi.generator.DataType;

public interface Column
{
    String getName();

    DataType getType();

    String build();
}
