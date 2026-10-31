package org.devlive.metaforge.service.body;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** What to export from one table and how: format plugin, row range and file options. */
@Data
@ToString
@NoArgsConstructor
public class TableExportBody
{
    // Name of an installed convert plugin, e.g. JsonConvert.
    private String format;
    // all exports every row; range exports `limit` rows from row `offset` (0-based).
    private String scope = "all";
    private long offset;
    private long limit = 5000;
    private boolean includeStructure;
    private boolean compress;
    private boolean excludeNull;
}
