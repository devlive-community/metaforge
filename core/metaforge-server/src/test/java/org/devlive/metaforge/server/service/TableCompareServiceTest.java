package org.devlive.metaforge.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.body.TableCompareBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.TableCompareService;
import org.devlive.metaforge.service.service.impl.TableCompareServiceImpl;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableCompareServiceTest
{
    private final List<String> sql = new ArrayList<>();

    /** app.report has id, name, status; bak.report has id, name (nullable), is_deleted. */
    private Response answer(String content)
    {
        sql.add(content);
        ObjectMapper mapper = new ObjectMapper();
        List<Object> rows = new ArrayList<>();
        if (content.contains("information_schema.COLUMNS")) {
            if (content.contains("TABLE_SCHEMA = 'app'")) {
                rows.add(Arrays.asList("id", "bigint", "NO", null, "主键"));
                rows.add(Arrays.asList("name", "varchar(100)", "NO", null, ""));
                rows.add(Arrays.asList("status", "tinyint", "NO", "1", ""));
            }
            else {
                rows.add(Arrays.asList("id", "bigint", "NO", null, "主键"));
                rows.add(Arrays.asList("name", "varchar(100)", "YES", null, ""));
                rows.add(Arrays.asList("is_deleted", "tinyint", "NO", "0", ""));
            }
        }
        else if (content.contains("KEY_COLUMN_USAGE")) {
            rows.add(mapper.createObjectNode().put("object_name", "id"));
        }
        else if (content.startsWith("SHOW CREATE TABLE")) {
            rows.add(mapper.createObjectNode().put("Create Table",
                    "CREATE TABLE `report` (\n  `id` bigint NOT NULL COMMENT '主键',\n  `name` varchar(100) NOT NULL,\n  `status` tinyint NOT NULL DEFAULT '1',\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB"));
        }
        else if (content.contains("FROM `app`.`report`")) {
            rows.add(new ArrayList<>(Arrays.asList(1, "a")));
            rows.add(new ArrayList<>(Arrays.asList(2, "b")));
        }
        else if (content.contains("FROM `bak`.`report`")) {
            rows.add(new ArrayList<>(Arrays.asList(1, "a2")));
            rows.add(new ArrayList<>(Arrays.asList(3, "c")));
        }
        return Response.builder().isSuccessful(true).columns(rows).build();
    }

    private TableCompareService service()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = new PluginService()
        {
            @Override
            public ManagementCapabilities managementCapabilities()
            {
                return ManagementCapabilities.forJdbc("mysql");
            }

            @Override
            public Response execute(RuntimeConfigure configure, String content)
            {
                return answer(content);
            }
        };
        SourceEntity entity = new SourceEntity();
        entity.setType("MySQL");
        when(repository.findByCode("demo")).thenReturn(Optional.of(entity));
        when(manager.getPlugin("MySQL")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        return new TableCompareServiceImpl(manager, repository);
    }

    private static TableCompareBody body()
    {
        TableCompareBody body = new TableCompareBody();
        body.setSourceDatabase("app");
        body.setSourceTable("report");
        body.setTargetDatabase("bak");
        body.setTargetTable("report");
        return body;
    }

    @Test
    public void structureCountsEachStatus()
    {
        CommonResponse<Map<String, Object>> response = service().structure("demo", body());
        assertTrue(response.getStatus(), String.valueOf(response.getMessage()));
        assertEquals(Map.of("changed", 1L, "source_only", 1L, "target_only", 1L, "same", 1L), response.getData().get("counts"));
    }

    @Test
    public void structureSyncAddsModifiesAndDropsFromTheSourceDefinitions()
    {
        TableCompareBody body = body();
        CommonResponse<org.devlive.metaforge.service.service.TableExportService.ExportFile> response = service().export("demo", body);
        assertTrue(response.getStatus(), String.valueOf(response.getMessage()));
        assertEquals("report-structure-sync.sql", response.getData().getName());
        assertEquals("-- Make `bak`.`report` match `app`.`report`\n"
                + "ALTER TABLE `bak`.`report` MODIFY COLUMN `name` varchar(100) NOT NULL;\n"
                + "ALTER TABLE `bak`.`report` ADD COLUMN `status` tinyint NOT NULL DEFAULT '1' AFTER `name`;\n"
                + "ALTER TABLE `bak`.`report` DROP COLUMN `is_deleted`;\n", new String(response.getData().getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void dataPairsByPrimaryKeyOverTheCommonColumnsAndRangesTheTarget()
    {
        TableCompareBody body = body();
        body.setScope("range");
        body.setOffset(0);
        body.setLimit(2);
        CommonResponse<Map<String, Object>> response = service().data("demo", body);
        assertTrue(response.getStatus(), String.valueOf(response.getMessage()));
        assertEquals(List.of("id", "name"), response.getData().get("columns"));
        assertEquals(Map.of("changed", 1L, "source_only", 1L, "target_only", 1L, "same", 0L), response.getData().get("counts"));
        assertTrue(sql.contains("SELECT `id`, `name` FROM `app`.`report` ORDER BY `id` LIMIT 2 OFFSET 0"), sql.toString());
        assertTrue(sql.contains("SELECT `id`, `name` FROM `bak`.`report` WHERE (`id`) >= (1) AND (`id`) <= (2) ORDER BY `id` LIMIT 200001"), sql.toString());
    }

    @Test
    public void dataSyncUpdatesInsertsAndDeletes()
    {
        TableCompareBody body = body();
        body.setKind("data");
        String content = new String(service().export("demo", body).getData().getContent(), StandardCharsets.UTF_8);
        assertEquals("-- Make the rows of `bak`.`report` match `app`.`report`\n"
                + "UPDATE `bak`.`report` SET `name` = 'a' WHERE `id` = 1;\n"
                + "INSERT INTO `bak`.`report` (`id`, `name`) VALUES (2, 'b');\n"
                + "DELETE FROM `bak`.`report` WHERE `id` = 3;\n", content);
    }

    @Test
    public void csvReportsQuoteValuesAndStartWithAByteOrderMark()
    {
        TableCompareBody body = body();
        body.setKind("data");
        body.setFormat("csv");
        String content = new String(service().export("demo", body).getData().getContent(), StandardCharsets.UTF_8);
        assertTrue(content.startsWith("﻿type,id (source),id (target),name (source),name (target)\nchanged,1,1,a,a2\n"), content);
        body.setMode("fields");
        assertFalse(service().data("demo", body).getStatus());
    }
}
