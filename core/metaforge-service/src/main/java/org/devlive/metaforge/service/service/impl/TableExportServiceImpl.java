package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.convert.ConvertFilter;
import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.body.TableExportBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.TableExportService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports table rows through the installed convert plugins. Rows are read raw (NoneConvert) and
 * handed to the chosen plugin's writer with the database column types, so the preview and the
 * file come from the same code.
 */
@Slf4j
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class TableExportServiceImpl
        implements TableExportService
{
    private static final int PREVIEW_ROWS = 5;
    private static final String RAW = "NoneConvert";

    private final PluginManager pluginManager;
    private final SourceRepository repository;

    public TableExportServiceImpl(PluginManager pluginManager, SourceRepository repository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
    }

    @Override
    public CommonResponse<Map<String, Object>> preview(String code, String database, String table, TableExportBody body)
    {
        return run(code, database, table, body, true, (context, file) -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            result.put("total", context.total);
            return result;
        });
    }

    @Override
    public CommonResponse<ExportFile> export(String code, String database, String table, TableExportBody body)
    {
        return run(code, database, table, body, false, (context, file) -> body.isCompress()
                ? new ExportFile(table + ".zip", zip(file))
                : new ExportFile(file.getName(), FileUtils.readFileToByteArray(file)));
    }

    private interface FileHandler<T>
    {
        T handle(Context context, File file)
                throws IOException;
    }

    private static final class Context
    {
        private long total;
    }

    private <T> CommonResponse<T> run(String code, String database, String table, TableExportBody body, boolean preview, FileHandler<T> handler)
    {
        String format = body.getFormat() == null ? "" : body.getFormat();
        if (format.isEmpty() || RAW.equals(format)) {
            return CommonResponse.failure("Choose an export format");
        }
        Optional<ConvertService> converter = ConvertFilter.filter(pluginManager, format);
        if (!converter.isPresent()) {
            return CommonResponse.failure("Export format [ " + format + " ] is not installed");
        }
        Optional<SourceEntity> source = repository.findByCode(code);
        if (!source.isPresent()) {
            return CommonResponse.failure(String.format("Resource [ %s ] not found", code));
        }
        return pluginManager.getPlugin(source.get().getType())
                .map(plugin -> {
                    PluginService service = plugin.getService(PluginService.class);
                    if (!service.managementCapabilities().supports("table:export")) {
                        return CommonResponse.<T>failure("Source does not support table:export");
                    }
                    RuntimeConfigure configure = source.get().toConfigure(RAW, pluginManager, plugin);
                    File directory = null;
                    try {
                        Context context = new Context();
                        String target = service.sqlIdentifier(database) + "." + service.sqlIdentifier(table);
                        context.total = count(service, configure, target);
                        Response rows = service.execute(configure, "SELECT * FROM " + target + range(body, preview));
                        if (rows == null || !Boolean.TRUE.equals(rows.getIsSuccessful())) {
                            return CommonResponse.<T>failure(rows == null ? "Reading the rows failed" : rows.getMessage());
                        }
                        directory = Files.createTempDirectory("metaforge-export").toFile();
                        ConvertRequest request = new ConvertRequest();
                        request.setName(table);
                        request.setPath(directory.getAbsolutePath());
                        request.setDelimiter("\t");
                        request.setHeaders(new ArrayList<>(rows.getHeaders()));
                        request.setColumns(rows.getColumns());
                        request.setTypes(types(service, configure, database, table, rows.getHeaders()));
                        request.setIncludeStructure(body.isIncludeStructure());
                        request.setExcludeNull(body.isExcludeNull());
                        ConvertResponse written = converter.get().writer(request);
                        if (!Boolean.TRUE.equals(written.getSuccessful())) {
                            return CommonResponse.<T>failure(written.getMessage());
                        }
                        File[] files = directory.listFiles();
                        if (files == null || files.length != 1) {
                            return CommonResponse.<T>failure("The export format wrote no file");
                        }
                        return CommonResponse.success(handler.handle(context, files[0]));
                    }
                    catch (IOException | RuntimeException error) {
                        log.warn("Export of {}.{} failed", database, table, error);
                        return CommonResponse.<T>failure(error.getMessage() == null ? "Export failed" : error.getMessage());
                    }
                    finally {
                        if (directory != null) {
                            FileUtils.deleteQuietly(directory);
                        }
                    }
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", source.get().getType())));
    }

    /** LIMIT/OFFSET for the preview (first rows of the range) or the chosen range; nothing for all rows. */
    static String range(TableExportBody body, boolean preview)
    {
        boolean ranged = "range".equals(body.getScope());
        long offset = ranged ? Math.max(0, body.getOffset()) : 0;
        if (preview) {
            long rows = ranged ? Math.min(PREVIEW_ROWS, Math.max(0, body.getLimit())) : PREVIEW_ROWS;
            return " LIMIT " + rows + " OFFSET " + offset;
        }
        if (!ranged) {
            return "";
        }
        if (body.getLimit() < 1) {
            throw new IllegalArgumentException("Export at least one row");
        }
        return " LIMIT " + body.getLimit() + " OFFSET " + offset;
    }

    private static long count(PluginService service, RuntimeConfigure configure, String target)
    {
        Response response = service.execute(configure, "SELECT COUNT(*) FROM " + target);
        if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful()) || response.getColumns() == null || response.getColumns().isEmpty()) {
            return -1;
        }
        Object row = response.getColumns().get(0);
        Object value = row instanceof List && !((List<?>) row).isEmpty() ? ((List<?>) row).get(0) : row;
        try {
            return Long.parseLong(String.valueOf(value));
        }
        catch (NumberFormatException error) {
            return -1;
        }
    }

    /** Database column types (e.g. BIGINT, VARCHAR(50)) in header order; empty where unknown. */
    private static List<Object> types(PluginService service, RuntimeConfigure configure, String database, String table, List<String> headers)
    {
        Map<String, String> byName = new HashMap<>();
        Response response = service.execute(configure, "SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = "
                + service.sqlLiteral(database) + " AND TABLE_NAME = " + service.sqlLiteral(table));
        if (response != null && Boolean.TRUE.equals(response.getIsSuccessful()) && response.getColumns() != null) {
            for (Object row : response.getColumns()) {
                if (row instanceof List && ((List<?>) row).size() >= 2) {
                    byName.put(String.valueOf(((List<?>) row).get(0)), String.valueOf(((List<?>) row).get(1)).toUpperCase(Locale.ROOT));
                }
                else if (row instanceof JsonNode) {
                    byName.put(((JsonNode) row).path("COLUMN_NAME").asText(), ((JsonNode) row).path("COLUMN_TYPE").asText().toUpperCase(Locale.ROOT));
                }
            }
        }
        List<Object> types = new ArrayList<>();
        headers.forEach(header -> types.add(byName.getOrDefault(header, "")));
        return types;
    }

    private static byte[] zip(File file)
            throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry(file.getName()));
            zip.write(FileUtils.readFileToByteArray(file));
            zip.closeEntry();
        }
        return buffer.toByteArray();
    }
}
