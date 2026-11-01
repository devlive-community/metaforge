package org.devlive.metaforge.server.importer;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.importer.ImportPlan;
import org.devlive.metaforge.service.importer.ImportStatements;
import org.devlive.metaforge.service.importer.ImportTable;
import org.devlive.metaforge.service.importer.SqlDumpReader;
import org.devlive.metaforge.service.importer.TypeInference;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.TableImportService;
import org.devlive.metaforge.service.service.impl.TableImportServiceImpl;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementCapability;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImportTest
{
    private static final String DUMP = "-- MySQL dump 10.13  Distrib 8.0.36, for Linux\n"
            + "DROP TABLE IF EXISTS `dataset`;\n"
            + "CREATE TABLE `dataset` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n  `name` varchar(128) NOT NULL,\n  `created_at` datetime DEFAULT NULL,\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB;\n"
            + "INSERT INTO `dataset` VALUES (1,'客户; 数据集 A','2026-10-28 10:30:00'),(2,'it\\'s ''quoted''',NULL);\n"
            + "INSERT INTO `logs` (`id`,`message`) VALUES (7,'hello'),(8,'world');\n";

    @Test
    public void dumpsGiveTablesColumnsAndUnescapedRows()
    {
        SqlDumpReader reader = SqlDumpReader.read(DUMP);
        assertEquals("MySQL 8.0", reader.getDialect());
        assertEquals(1, reader.getSkipped());
        List<ImportTable> tables = reader.getTables();
        assertEquals(List.of("dataset", "logs"), tables.stream().map(ImportTable::getName).collect(java.util.stream.Collectors.toList()));
        ImportTable dataset = tables.get(0);
        assertEquals(List.of("BIGINT", "VARCHAR(128)", "DATETIME"), dataset.getColumns().stream().map(ImportTable.ImportColumn::getType).collect(java.util.stream.Collectors.toList()));
        assertEquals(Arrays.asList("1", "客户; 数据集 A", "2026-10-28 10:30:00"), dataset.getRows().get(0));
        assertEquals(Arrays.asList("2", "it's 'quoted'", null), dataset.getRows().get(1));
        assertEquals("BIGINT", tables.get(1).getColumns().get(0).getType());
        assertEquals("VARCHAR(64)", tables.get(1).getColumns().get(1).getType());
    }

    @Test
    public void typesAreCompatibleWithinAFamilyOrIntoText()
    {
        assertTrue(TypeInference.compatible("INT", "BIGINT"));
        assertTrue(TypeInference.compatible("BIGINT", "DECIMAL(10,2)"));
        assertTrue(TypeInference.compatible("JSON", "TEXT"));
        assertFalse(TypeInference.compatible("JSON", "INT"));
        assertFalse(TypeInference.compatible("VARCHAR(10)", "DATETIME"));
    }

    @Test
    public void insertsBatchRowsAndApplyTheConflictStrategy()
    {
        ImportTable table = SqlDumpReader.read(DUMP).getTables().get(0);
        ImportPlan.TablePlan plan = new ImportPlan.TablePlan();
        plan.setSource("dataset");
        plan.setTarget("datacap_dataset");
        plan.setMappings(List.of(mapping("id", "id", "keep"), mapping("name", "title", "direct"), mapping("created_at", "created", "date")));
        ImportPlan.Settings settings = new ImportPlan.Settings();
        settings.setBatchSize(1);
        settings.setNullMode("nullToEmpty");
        ImportStatements statements = new ImportStatements(name -> "`" + name + "`", value -> "'" + value.replace("'", "''") + "'");
        List<String> skip = statements.inserts("app", table, plan, settings, "skip");
        assertEquals(2, skip.size());
        assertEquals("INSERT IGNORE INTO `app`.`datacap_dataset` (`id`, `title`, `created`) VALUES\n  ('1', '客户; 数据集 A', '2026-10-28 10:30:00')", skip.get(0));
        assertEquals("INSERT IGNORE INTO `app`.`datacap_dataset` (`id`, `title`, `created`) VALUES\n  ('2', 'it''s ''quoted''', '')", skip.get(1));
        assertTrue(statements.inserts("app", table, plan, settings, "upsert").get(0)
                .endsWith("ON DUPLICATE KEY UPDATE `id` = VALUES(`id`), `title` = VALUES(`title`), `created` = VALUES(`created`)"));
        assertTrue(statements.create("app", table, "copy").startsWith("CREATE TABLE `app`.`copy` (\n  `id` bigint NOT NULL AUTO_INCREMENT"));
    }

    private static ImportPlan.FieldMapping mapping(String source, String target, String handling)
    {
        ImportPlan.FieldMapping mapping = new ImportPlan.FieldMapping();
        mapping.setSource(source);
        mapping.setTarget(target);
        mapping.setHandling(handling);
        return mapping;
    }

    // ---- service ----

    private final List<String> executed = Collections.synchronizedList(new ArrayList<>());

    private TableImportService service()
    {
        return service(ManagementCapability.keys());
    }

    private TableImportService service(Set<String> capabilities)
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = new PluginService()
        {
            @Override
            public ManagementCapabilities managementCapabilities()
            {
                return new ManagementCapabilities("relational", capabilities);
            }

            @Override
            public Response execute(RuntimeConfigure configure, String content)
            {
                executed.add(content);
                List<Object> rows = new ArrayList<>();
                if (content.contains("information_schema.COLUMNS")) {
                    rows.add(Arrays.asList("datacap_dataset", "id", "bigint", "NO", null, "auto_increment"));
                    rows.add(Arrays.asList("datacap_dataset", "name", "varchar(128)", "NO", null, ""));
                    rows.add(Arrays.asList("datacap_dataset", "owner", "varchar(32)", "NO", null, ""));
                }
                else {
                    rows.add(List.of(2));
                }
                return Response.builder().isSuccessful(true).columns(rows).build();
            }
        };
        SourceEntity entity = new SourceEntity();
        entity.setType("MySQL");
        when(repository.findByCode("demo")).thenReturn(Optional.of(entity));
        when(manager.getPlugin("MySQL")).thenReturn(Optional.of(plugin));
        when(manager.getPluginInfos()).thenReturn(List.of());
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        return new TableImportServiceImpl(manager, repository);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void analyseThenPreviewFlagsMissingRequiredFieldsAndNewTables()
    {
        TableImportService service = service();
        CommonResponse<Map<String, Object>> analysed = service.analyze("demo", "app", new MockMultipartFile("file", "backup.sql", "text/plain", DUMP.getBytes(StandardCharsets.UTF_8)), null);
        assertTrue(analysed.getStatus(), String.valueOf(analysed.getMessage()));
        Map<String, Object> file = (Map<String, Object>) analysed.getData().get("file");
        assertEquals("SqlDump", file.get("parser"));
        assertEquals("UTF-8", file.get("encoding"));
        assertEquals(2, ((List<?>) analysed.getData().get("tables")).size());

        ImportPlan plan = new ImportPlan();
        plan.setImportId(String.valueOf(analysed.getData().get("importId")));
        ImportPlan.TablePlan existing = new ImportPlan.TablePlan();
        existing.setSource("dataset");
        existing.setTarget("datacap_dataset");
        existing.setMappings(List.of(mapping("id", "id", "keep"), mapping("name", "name", "direct")));
        ImportPlan.TablePlan created = new ImportPlan.TablePlan();
        created.setSource("logs");
        created.setAction("create");
        created.setTarget("logs_import");
        created.setMappings(List.of(mapping("id", "id", "direct"), mapping("message", "message", "direct")));
        plan.setTables(List.of(existing, created));
        CommonResponse<Map<String, Object>> preview = service.preview("demo", "app", plan);
        assertTrue(preview.getStatus(), String.valueOf(preview.getMessage()));
        List<Map<String, Object>> tables = (List<Map<String, Object>>) preview.getData().get("tables");
        assertEquals("error", tables.get(0).get("status"));
        assertEquals("requiredUnmapped", ((List<Map<String, Object>>) tables.get(0).get("messages")).get(0).get("code"));
        assertEquals("pass", tables.get(1).get("status"));
        assertTrue(String.valueOf(preview.getData().get("sql")).startsWith("CREATE TABLE `app`.`logs_import`"));
        assertFalse(service.execute("demo", "app", plan).getStatus());
        assertFalse(service.preview("demo", "other", plan).getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void aValidPlanRunsAsAJobThatCreatesThenInserts()
            throws InterruptedException
    {
        TableImportService service = service();
        Map<String, Object> analysed = service.analyze("demo", "app", new MockMultipartFile("file", "backup.sql", "text/plain", DUMP.getBytes(StandardCharsets.UTF_8)), null).getData();
        ImportPlan plan = new ImportPlan();
        plan.setImportId(String.valueOf(analysed.get("importId")));
        ImportPlan.TablePlan created = new ImportPlan.TablePlan();
        created.setSource("logs");
        created.setAction("create");
        created.setTarget("logs_import");
        created.setMappings(List.of(mapping("id", "id", "direct"), mapping("message", "message", "direct")));
        plan.setTables(List.of(created));
        CommonResponse<Map<String, Object>> started = service.execute("demo", "app", plan);
        assertTrue(started.getStatus(), String.valueOf(started.getMessage()));
        String id = String.valueOf(started.getData().get("jobId"));
        Map<String, Object> job = service.job(id).getData();
        for (int attempt = 0; attempt < 50 && "running".equals(job.get("status")); attempt++) {
            Thread.sleep(20);
            job = service.job(id).getData();
        }
        assertEquals("done", job.get("status"));
        Map<String, Object> table = ((List<Map<String, Object>>) job.get("tables")).get(0);
        assertEquals(2L, table.get("written"));
        assertEquals(0L, table.get("skipped"));
        assertTrue(executed.stream().anyMatch(sql -> sql.startsWith("CREATE TABLE `app`.`logs_import`")));
        assertTrue(executed.stream().anyMatch(sql -> sql.startsWith("INSERT IGNORE INTO `app`.`logs_import` (`id`, `message`) VALUES")));
    }

    @Test
    public void aTableEmptiedBeforeItsImportNeedsTheTruncateCapability()
    {
        Set<String> capabilities = new LinkedHashSet<>(ManagementCapability.keys());
        capabilities.remove("table:truncate");
        TableImportService service = service(capabilities);
        Map<String, Object> analysed = service.analyze("demo", "app", new MockMultipartFile("file", "backup.sql", "text/plain", DUMP.getBytes(StandardCharsets.UTF_8)), null).getData();
        ImportPlan plan = new ImportPlan();
        plan.setImportId(String.valueOf(analysed.get("importId")));
        ImportPlan.TablePlan table = new ImportPlan.TablePlan();
        table.setSource("dataset");
        table.setTarget("datacap_dataset");
        table.setWriteMode("truncate");
        table.setMappings(List.of(mapping("id", "id", "keep"), mapping("name", "name", "direct")));
        plan.setTables(List.of(table));
        CommonResponse<Map<String, Object>> started = service.execute("demo", "app", plan);
        assertFalse(started.getStatus());
        assertEquals("Source does not support table:truncate", started.getMessage());
    }
}
