package org.devlive.metaforge.spi;

import org.devlive.metaforge.spi.generator.DataType;
import org.devlive.metaforge.spi.generator.definition.ColumnDefinition;
import org.junit.Assert;
import org.junit.Test;

public class SourceColumnDefinitionTest
{
    private final PluginService service = new PluginService() {};

    @Test
    public void decimalPrecisionSurvivesColumnReordering()
    {
        ColumnDefinition column = new ColumnDefinition();
        column.setName("amount");
        column.setType(DataType.DECIMAL);
        column.setPrecision(12);
        column.setScale(3);
        column.setNullable(false);
        column.setDefaultValue("0");
        column.setComment("Order's total");
        column.setAfter("id");
        Assert.assertEquals("`amount` DECIMAL(12,3) NOT NULL DEFAULT '0' COMMENT 'Order''s total' AFTER `id`", service.getCreateColumn(column).build());
    }

    @Test
    public void autoIncrementPrecedesFirstPosition()
    {
        ColumnDefinition column = new ColumnDefinition();
        column.setName("id");
        column.setType(DataType.BIGINT);
        column.setNullable(false);
        column.setAutoIncrement(true);
        column.setFirst(true);
        Assert.assertEquals("`id` BIGINT NOT NULL AUTO_INCREMENT FIRST", service.getCreateColumn(column).build());
    }

    @Test
    public void jsonAndEmptyDefaultArePreserved()
    {
        ColumnDefinition column = new ColumnDefinition();
        column.setName("config");
        column.setType(DataType.JSON);
        Assert.assertEquals("`config` JSON", service.getCreateColumn(column).build());
        column.setType(DataType.VARCHAR);
        column.setLength(128);
        column.setDefaultValue("");
        Assert.assertEquals("`config` VARCHAR(128) DEFAULT ''", service.getCreateColumn(column).build());
    }
}
