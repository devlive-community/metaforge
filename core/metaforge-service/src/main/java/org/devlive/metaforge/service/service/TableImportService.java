package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.importer.ImportPlan;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TableImportService
{
    /** Formats the import can read: SQL dumps, ZIP archives and the installed convert plugins. */
    CommonResponse<List<Map<String, Object>>> parsers();

    /** Reads an uploaded file and describes the tables, columns and rows it holds, kept for the next steps. */
    CommonResponse<Map<String, Object>> analyze(String code, String database, MultipartFile file, String parser);

    /** Checks a plan against the database and shows sample rows and the SQL it would run. */
    CommonResponse<Map<String, Object>> preview(String code, String database, ImportPlan plan);

    /** Starts the import in the background and returns its job id. */
    CommonResponse<Map<String, Object>> execute(String code, String database, ImportPlan plan);

    /** Progress and result of an import job. */
    CommonResponse<Map<String, Object>> job(String id);
}
