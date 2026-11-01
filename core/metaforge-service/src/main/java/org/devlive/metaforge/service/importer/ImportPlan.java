package org.devlive.metaforge.service.importer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** What the import wizard decided: per source table its target and field mapping, and the settings. */
@Data
@NoArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ImportPlan
{
    private String importId;
    private List<TablePlan> tables = new ArrayList<>();
    private Settings settings = new Settings();

    @Data
    @NoArgsConstructor
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    public static class TablePlan
    {
        private String source;
        // existing writes into `target`; create creates `target` first; skip leaves the table out.
        private String action = "existing";
        private String target;
        private List<FieldMapping> mappings = new ArrayList<>();
        // Overrides of the settings' write mode and conflict strategy for this table; empty keeps them.
        private String writeMode;
        private String conflict;
    }

    @Data
    @NoArgsConstructor
    public static class FieldMapping
    {
        private String source;
        // Empty leaves the source field out.
        private String target;
        // keep or direct write the value as read; date normalises it to a datetime; ignore leaves it out.
        private String handling = "direct";
    }

    @Data
    @NoArgsConstructor
    public static class Settings
    {
        // append, truncate (empty the target first) or structure (create the new tables only).
        private String writeMode = "append";
        // skip (INSERT IGNORE), upsert (ON DUPLICATE KEY UPDATE) or error (plain INSERT).
        private String conflict = "skip";
        private int batchSize = 1000;
        private int parallel = 1;
        private String timeFormat = "yyyy-MM-dd HH:mm:ss";
        // keep, emptyToNull or nullToEmpty.
        private String nullMode = "keep";
        private boolean checkBefore = true;
        private boolean transactional = true;
        private boolean logSkipped = true;
        private boolean continueOnError;
    }
}
