package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.TableExportBody;

import java.util.Map;

public interface TableExportService
{
    /** The first rows of the export as the file would hold them, with the table's row count. */
    CommonResponse<Map<String, Object>> preview(String code, String database, String table, TableExportBody body);

    /** The export file: its name and bytes, a ZIP archive when compression is asked for. */
    CommonResponse<ExportFile> export(String code, String database, String table, TableExportBody body);

    final class ExportFile
    {
        private final String name;
        private final byte[] content;

        public ExportFile(String name, byte[] content)
        {
            this.name = name;
            this.content = content.clone();
        }

        public String getName()
        {
            return name;
        }

        public byte[] getContent()
        {
            return content.clone();
        }
    }
}
