package org.devlive.metaforge.server.exporter;

import org.apache.commons.io.FileUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.model.ConvertOption;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.exporter.ExportPlan;
import org.devlive.metaforge.service.exporter.ExportStatements;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.DatabaseExportService;
import org.devlive.metaforge.service.service.TableExportService;
import org.devlive.metaforge.service.service.impl.DatabaseExportServiceImpl;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementCapability;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseExportTest
{
    private final ExportStatements statements = new ExportStatements(name -> "`" + name + "`", value -> "'" + value.replace("'", "''") + "'");

    private static ExportPlan.Filter filter(String column, String operator, String value)
    {
        ExportPlan.Filter filter = new ExportPlan.Filter();
        filter.setColumn(column);
        filter.setOperator(operator);
        filter.setValue(value);
        return filter;
    }

    private static ExportPlan.TablePlan table(String name, List<String> fields, ExportPlan.Filter... filters)
    {
        ExportPlan.TablePlan table = new ExportPlan.TablePlan();
        table.setName(name);
        table.setFields(new ArrayList<>(fields));
        table.setFilters(new ArrayList<>(List.of(filters)));
        return table;
    }

    @Test
    public void selectsTakeFieldsFiltersAndPages()
    {
        ExportPlan.TablePlan chat = table("chat", List.of("id", "title"), filter("created", ">=", "2026-10-01"), filter("title", "IS NOT NULL", null));
        assertEquals("SELECT `id`, `title` FROM `app`.`chat` WHERE `created` >= '2026-10-01' AND `title` IS NOT NULL LIMIT 1000 OFFSET 2000",
                statements.select("app", chat, 1000, 2000));
        assertEquals("SELECT * FROM `app`.`logs`", statements.select("app", table("logs", List.of()), 0, 0));
        assertEquals("SELECT COUNT(*) FROM `app`.`chat` WHERE `title` LIKE 'it''s%'", statements.count("app", table("chat", List.of(), filter("title", "like", "it's%"))));
    }

    @Test
    public void theSchemaDropsCreatesAndCanLeaveIndexesOut()
    {
        String create = "CREATE TABLE `chat` (\n  `id` bigint NOT NULL,\n  `user_id` bigint,\n  PRIMARY KEY (`id`),\n  KEY `idx_user` (`user_id`),\n"
                + "  CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)\n) ENGINE=InnoDB";
        ExportPlan.Structure structure = new ExportPlan.Structure();
        structure.setDrop(true);
        structure.setIndexes(false);
        assertEquals("DROP TABLE IF EXISTS `app`.`chat`;\nCREATE TABLE `chat` (\n  `id` bigint NOT NULL,\n  `user_id` bigint,\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB;\n",
                statements.schema("app", "chat", create, structure));
        structure.setIndexes(true);
        structure.setDrop(false);
        assertTrue(statements.schema("app", "chat", create, structure).contains("KEY `idx_user`"));
    }

    // ---- service ----

    private final List<String> executed = Collections.synchronizedList(new ArrayList<>());

    /** A format writing "headers|rows|options" as a .fake file. */
    private static final class FakeConvert
            implements ConvertService
    {
        @Override
        public List<ConvertOption> options()
        {
            return List.of(new ConvertOption("indent", Map.of("en", "Indent"), "2", List.of()));
        }

        @Override
        public String extension()
        {
            return "fake";
        }

        @Override
        public ConvertResponse format(ConvertRequest request)
        {
            return new ConvertResponse();
        }

        @Override
        public ConvertResponse formatStream(ConvertRequest request)
        {
            return new ConvertResponse();
        }

        @Override
        public ConvertResponse writer(ConvertRequest request)
        {
            ConvertResponse response = new ConvertResponse();
            try {
                FileUtils.writeStringToFile(new File(request.getPath(), request.getName() + ".fake"), request.getHeaders() + "|" + request.getColumns() + "|" + request.getOptions(),
                        StandardCharsets.UTF_8);
                response.setSuccessful(true);
            }
            catch (IOException error) {
                response.setSuccessful(false);
            }
            return response;
        }

        @Override
        public ConvertResponse reader(ConvertRequest request)
        {
            return new ConvertResponse();
        }
    }

    private Response answer(String sql)
    {
        executed.add(sql);
        List<Object> rows = new ArrayList<>();
        if (sql.contains("information_schema.COLUMNS")) {
            rows.add(List.of("chat", "id", "bigint"));
            rows.add(List.of("chat", "title", "varchar(64)"));
            rows.add(List.of("logs", "id", "bigint"));
            return Response.builder().isSuccessful(true).headers(List.of("TABLE_NAME", "COLUMN_NAME", "COLUMN_TYPE")).columns(rows).build();
        }
        if (sql.startsWith("SHOW CREATE TABLE")) {
            String table = sql.contains("`chat`") ? "chat" : "logs";
            rows.add(List.of(table, "CREATE TABLE `" + table + "` (\n  `id` bigint NOT NULL,\n  KEY `idx` (`id`)\n)"));
            return Response.builder().isSuccessful(true).headers(List.of("Table", "Create Table")).columns(rows).build();
        }
        if (sql.startsWith("SELECT COUNT(*)")) {
            rows.add(List.of(3));
            return Response.builder().isSuccessful(true).headers(List.of("COUNT(*)")).columns(rows).build();
        }
        // Three rows, served a page at a time.
        Matcher page = Pattern.compile("LIMIT (\\d+) OFFSET (\\d+)$").matcher(sql);
        long limit = page.find() ? Long.parseLong(page.group(1)) : 3;
        long offset = page.find(0) ? Long.parseLong(page.group(2)) : 0;
        for (long id = offset + 1; id <= Math.min(3, offset + limit); id++) {
            rows.add(List.of(id, "t" + id));
        }
        return Response.builder().isSuccessful(true).headers(List.of("id", "title")).columns(rows).build();
    }

    private DatabaseExportService service()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin source = mock(Plugin.class);
        Plugin convert = mock(Plugin.class);
        PluginService adapter = new PluginService()
        {
            @Override
            public ManagementCapabilities managementCapabilities()
            {
                return new ManagementCapabilities("relational", ManagementCapability.keys());
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
        when(manager.getPlugin("MySQL")).thenReturn(Optional.of(source));
        when(manager.getPlugin("FakeConvert")).thenReturn(Optional.of(convert));
        when(manager.getPluginInfos()).thenReturn(List.of());
        when(convert.getService(ConvertService.class)).thenReturn(new FakeConvert());
        when(source.getService(PluginService.class)).thenReturn(adapter);
        return new DatabaseExportServiceImpl(manager, repository);
    }

    private static ExportPlan plan(ExportPlan.TablePlan... tables)
    {
        ExportPlan plan = new ExportPlan();
        plan.setTables(new ArrayList<>(List.of(tables)));
        plan.setFormat("FakeConvert");
        plan.setOptions(Map.of("indent", "4"));
        plan.setFileName("app export");
        plan.setBatchSize(2);
        plan.getStructure().setDrop(true);
        plan.getStructure().setIndexes(false);
        return plan;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void theCheckFlagsUnknownFieldsAndNamesTheFiles()
    {
        DatabaseExportService service = service();
        CommonResponse<Map<String, Object>> checked = service.check("demo", "app", plan(table("chat", List.of("id", "missing")), table("logs", List.of()), table("ghost", List.of())));
        assertTrue(checked.getStatus(), String.valueOf(checked.getMessage()));
        List<Map<String, Object>> tables = (List<Map<String, Object>>) checked.getData().get("tables");
        assertEquals(List.of("error", "pass", "error"), tables.stream().map(row -> row.get("status")).collect(java.util.stream.Collectors.toList()));
        assertEquals(List.of("Field missing does not exist"), tables.get(0).get("messages"));
        assertEquals(List.of("chat.fake", "logs.fake", "ghost.fake", "schema.sql"), checked.getData().get("files"));
        assertEquals("app_export.zip", checked.getData().get("output"));
        assertFalse(service.execute("demo", "app", plan(table("chat", List.of("missing")))).getStatus());
    }

    @Test
    public void theSampleIsWrittenByTheChosenFormatWithItsOptions()
    {
        CommonResponse<Map<String, Object>> sample = service().sample("demo", "app", plan(table("chat", List.of("id", "title"))), "chat", 3);
        assertTrue(sample.getStatus(), String.valueOf(sample.getMessage()));
        assertEquals("[id, title]|[[1, t1], [2, t2], [3, t3]]|{indent=4}", sample.getData().get("content"));
        assertTrue(executed.contains("SELECT `id`, `title` FROM `app`.`chat` LIMIT 3 OFFSET 0"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void aJobReadsInPagesAndZipsEveryTableWithTheSchema()
            throws Exception
    {
        DatabaseExportService service = service();
        CommonResponse<Map<String, Object>> started = service.execute("demo", "app", plan(table("chat", List.of()), table("logs", List.of())));
        assertTrue(started.getStatus(), String.valueOf(started.getMessage()));
        String id = String.valueOf(started.getData().get("jobId"));
        Map<String, Object> job = service.job(id).getData();
        for (int attempt = 0; attempt < 100 && "running".equals(job.get("status")); attempt++) {
            Thread.sleep(20);
            job = service.job(id).getData();
        }
        assertEquals("done", job.get("status"), String.valueOf(job.get("message")));
        assertEquals("app_export.zip", job.get("file"));
        Map<String, Object> chat = ((List<Map<String, Object>>) job.get("tables")).get(0);
        assertEquals(3L, chat.get("written"));
        assertTrue(executed.contains("SELECT * FROM `app`.`chat` LIMIT 2 OFFSET 2"));

        TableExportService.ExportFile file = service.file(id).getData();
        assertEquals("app_export.zip", file.getName());
        Map<String, String> entries = new TreeMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file.getContent()))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertEquals(List.of("chat.fake", "logs.fake", "schema.sql"), new ArrayList<>(entries.keySet()));
        assertEquals("[id, title]|[[1, t1], [2, t2], [3, t3]]|{indent=4}", entries.get("chat.fake"));
        assertEquals("DROP TABLE IF EXISTS `app`.`chat`;\nCREATE TABLE `chat` (\n  `id` bigint NOT NULL\n);\n\nDROP TABLE IF EXISTS `app`.`logs`;\nCREATE TABLE `logs` (\n  `id` bigint NOT NULL\n);\n",
                entries.get("schema.sql"));
        assertTrue(service.job(id).getStatus(), "kept for two hours");
    }
}
