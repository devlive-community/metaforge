package org.devlive.metaforge.server.service;

import org.apache.commons.io.FileUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.body.TableExportBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.TableExportService;
import org.devlive.metaforge.service.service.impl.TableExportServiceImpl;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableExportServiceTest
{
    private final List<String> sql = new ArrayList<>();

    /** Writes one line per row plus the options and types it was given, so the test can read them back. */
    public static class FakeConvert
            implements ConvertService
    {
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
                FileUtils.writeStringToFile(new File(request.getPath(), request.getName() + ".fake"), request.getHeaders() + " " + request.getTypes() + " "
                        + request.getIncludeStructure() + " " + request.getExcludeNull() + " " + request.getColumns(), StandardCharsets.UTF_8);
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

    private Response answer(String content)
    {
        sql.add(content);
        List<Object> rows = new ArrayList<>();
        if (content.startsWith("SELECT COUNT(*)")) {
            rows.add(List.of(3215L));
            return Response.builder().isSuccessful(true).headers(List.of("COUNT(*)")).columns(rows).build();
        }
        if (content.contains("information_schema.COLUMNS")) {
            rows.add(List.of("id", "bigint"));
            return Response.builder().isSuccessful(true).headers(List.of("COLUMN_NAME", "COLUMN_TYPE")).columns(rows).build();
        }
        rows.add(new ArrayList<>(Arrays.asList(1, null)));
        return Response.builder().isSuccessful(true).headers(List.of("id", "name")).columns(rows).build();
    }

    private TableExportService service()
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
        when(manager.getPlugin("MySQL")).thenReturn(Optional.of(source));
        when(manager.getPlugin("FakeConvert")).thenReturn(Optional.of(convert));
        when(manager.getPlugin("NoneConvert")).thenReturn(Optional.of(convert));
        when(convert.getService(ConvertService.class)).thenReturn(new FakeConvert());
        when(source.getService(PluginService.class)).thenReturn(adapter);
        return new TableExportServiceImpl(manager, repository);
    }

    private static TableExportBody body(String scope, boolean compress)
    {
        TableExportBody body = new TableExportBody();
        body.setFormat("FakeConvert");
        body.setScope(scope);
        body.setOffset(10);
        body.setLimit(3);
        body.setIncludeStructure(true);
        body.setCompress(compress);
        return body;
    }

    @Test
    public void previewReadsTheFirstRowsOfTheRangeWithTypesAndTheRowCount()
    {
        CommonResponse<Map<String, Object>> response = service().preview("demo", "app", "user", body("range", false));
        assertTrue(response.getStatus(), String.valueOf(response.getMessage()));
        assertEquals(3215L, response.getData().get("total"));
        assertEquals("[id, name] [BIGINT, ] true false [[1, null]]", response.getData().get("content"));
        assertEquals("SELECT * FROM `app`.`user` LIMIT 3 OFFSET 10", sql.get(1));
    }

    @Test
    public void allRowsExportWithoutALimitAndCompressIntoAZip()
    {
        CommonResponse<TableExportService.ExportFile> response = service().export("demo", "app", "user", body("all", true));
        assertTrue(response.getStatus(), String.valueOf(response.getMessage()));
        assertEquals("SELECT * FROM `app`.`user`", sql.get(1));
        assertEquals("user.zip", response.getData().getName());
        byte[] head = new byte[2];
        System.arraycopy(response.getData().getContent(), 0, head, 0, 2);
        assertArrayEquals(new byte[] {'P', 'K'}, head);
    }

    @Test
    public void rawRowsAreNotAnExportFormat()
    {
        TableExportBody body = body("all", false);
        body.setFormat("NoneConvert");
        assertFalse(service().preview("demo", "app", "user", body).getStatus());
    }
}
