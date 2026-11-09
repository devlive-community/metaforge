package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.exporter.ExportPlan;

import java.util.List;
import java.util.Map;

public interface DatabaseExportService
{
    /** Installed export formats with their file extension, version and the settings they declare. */
    CommonResponse<List<Map<String, Object>>> formats();

    /** The first rows of one table as read and as the chosen format writes them. */
    CommonResponse<Map<String, Object>> sample(String code, String database, ExportPlan plan, String table, int rows);

    /** Checks every table of a plan and lists the files the export will write. */
    CommonResponse<Map<String, Object>> check(String code, String database, ExportPlan plan);

    /** Starts the export in the background and returns its job id. */
    CommonResponse<Map<String, Object>> execute(String code, String database, ExportPlan plan);

    /** Progress and result of an export job. */
    CommonResponse<Map<String, Object>> job(String id);

    /** The finished file of an export job. */
    CommonResponse<TableExportService.ExportFile> file(String id);
}
