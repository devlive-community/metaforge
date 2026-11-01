package org.devlive.metaforge.service.body;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/** Two tables of one source to compare, and how to compare or export their data. */
@Data
@ToString
@NoArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableCompareBody
{
    private String sourceDatabase;
    private String sourceTable;
    private String targetDatabase;
    private String targetTable;
    // primary pairs rows by the source table's primary key; fields pairs them by `keys`.
    private String mode = "primary";
    private List<String> keys = new ArrayList<>();
    // all compares every row; range compares `limit` source rows from row `offset` (0-based) in key order.
    private String scope = "all";
    private long offset;
    private long limit = 5000;
    private boolean onlyChanged = true;
    private boolean includeMissing = true;
    // Exports: structure or data, as sql (statements making the target match) or csv (a report).
    private String kind = "structure";
    private String format = "sql";
}
