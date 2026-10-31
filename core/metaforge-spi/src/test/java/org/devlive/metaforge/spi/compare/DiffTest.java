package org.devlive.metaforge.spi.compare;

import org.devlive.metaforge.spi.sql.TableDdl;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiffTest
{
    @Test
    public void structurePairsColumnsByNameAndNamesTheDifferences()
    {
        List<Map<String, Object>> diff = StructureDiff.compare(
                Arrays.asList(new StructureDiff.Column("id", "bigint", false, null, "主键"), new StructureDiff.Column("report_type", "varchar(50)", false, "chart", ""),
                        new StructureDiff.Column("status", "tinyint", false, "1", "")),
                Arrays.asList(new StructureDiff.Column("id", "BIGINT", false, null, "主键"), new StructureDiff.Column("report_type", "varchar(50)", true, null, ""),
                        new StructureDiff.Column("is_deleted", "tinyint", false, "0", "")));
        Assert.assertEquals(Arrays.asList("same", "changed", "source_only", "target_only"), diff.stream().map(row -> row.get("status")).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("nullable", "defaultValue"), diff.get(1).get("changes"));
        Assert.assertNull(diff.get(2).get("target"));
        Assert.assertNull(diff.get(3).get("source"));
    }

    @Test
    public void dataPairsRowsByKeyAndListsChangedColumns()
    {
        List<String> columns = Arrays.asList("id", "name", "updated");
        DataDiff.Result result = DataDiff.compare(columns, Arrays.asList("id"),
                Arrays.asList(Arrays.asList(1, "a", "x"), Arrays.asList(2, "b", null), Arrays.asList(3, "c", "z")),
                Arrays.asList(Arrays.asList(1L, "a", "x2"), Arrays.asList(3, "c", "z"), Arrays.asList(4, "d", "w")), false);
        Assert.assertEquals(1, result.getSame());
        Assert.assertEquals(1, result.count(DataDiff.CHANGED));
        Assert.assertEquals(Arrays.asList("updated"), result.getRows().get(0).getChanged());
        Assert.assertEquals(DataDiff.SOURCE_ONLY, result.getRows().get(1).getType());
        Assert.assertEquals(DataDiff.TARGET_ONLY, result.getRows().get(2).getType());
        // Identical pairs are listed only when asked for.
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, "a", "x"), Arrays.asList(3, "c", "z"));
        Assert.assertEquals(2, DataDiff.compare(columns, Arrays.asList("id"), rows, rows, true).getRows().size());
    }

    @Test
    public void columnDefinitionsKeepTheirTextByName()
    {
        Map<String, String> definitions = TableDdl.parse("CREATE TABLE `t` (\n  `id` bigint NOT NULL,\n  `a``b` int DEFAULT NULL,\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB")
                .columnDefinitions();
        Assert.assertEquals(Arrays.asList("id", "a`b"), new ArrayList<>(definitions.keySet()));
        Assert.assertEquals("`a``b` int DEFAULT NULL", definitions.get("a`b"));
    }
}
