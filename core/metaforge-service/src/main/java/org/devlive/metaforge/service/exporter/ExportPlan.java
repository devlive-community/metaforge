package org.devlive.metaforge.service.exporter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What a database export writes: which tables, their fields and rows, the format and the output. */
@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ExportPlan
{
    private List<TablePlan> tables = new ArrayList<>();
    /** all: structure and data; data: data only. */
    private String content = "all";
    private Structure structure = new Structure();
    /** An installed convert plugin, e.g. JsonConvert. */
    private String format;
    /** Values of the plugin's declared options. */
    private Map<String, String> options = new LinkedHashMap<>();
    private String fileName;
    /** zip: a file per table packed into one ZIP; merge: one file for every table (only for plugins that merge). */
    private String organize = "zip";
    private boolean compress = true;
    private int batchSize = 1000;
    private int concurrency = 1;
    /** batch: read each table in pages of batchSize; single: one query per table. */
    private String reading = "batch";
    /** stop: end the job at the first failing table; skip: export the others. */
    private String failure = "stop";
    /** Keep the job and its file for two hours after the download. */
    private boolean keepRecord = true;

    @Data
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static class TablePlan
    {
        private String name;
        /** Fields to export; empty exports every field. */
        private List<String> fields = new ArrayList<>();
        /** Conditions the rows must all meet; empty exports every row. */
        private List<Filter> filters = new ArrayList<>();
    }

    @Data
    public static class Filter
    {
        private String column;
        /** =, !=, >, >=, <, <=, LIKE, NOT LIKE, IS NULL, IS NOT NULL */
        private String operator = "=";
        private String value;
    }

    @Data
    public static class Structure
    {
        private boolean create = true;
        private boolean indexes = true;
        private boolean drop;
    }

    public boolean includesStructure()
    {
        return "all".equals(content) && (structure.isCreate() || structure.isDrop());
    }
}
