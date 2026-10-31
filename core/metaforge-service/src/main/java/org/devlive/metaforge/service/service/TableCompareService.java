package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.TableCompareBody;

import java.util.Map;

public interface TableCompareService
{
    /** Column by column differences with counts per status. */
    CommonResponse<Map<String, Object>> structure(String code, TableCompareBody body);

    /** Differing rows (at most a page of them) with counts, the compared columns and the key. */
    CommonResponse<Map<String, Object>> data(String code, TableCompareBody body);

    /** The statements making the target match the source, or a CSV report, as a file. */
    CommonResponse<TableExportService.ExportFile> export(String code, TableCompareBody body);
}
