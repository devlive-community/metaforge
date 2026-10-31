package org.devlive.metaforge.service.body;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.enums.FunctionType;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class FunctionsImportBody
{
    @NotNull
    @Size(min = 1)
    private List<String> plugin;

    private FunctionType type = FunctionType.KEYWORD;

    /**
     * One map per SQL knowledge entry. Entries do not have to share the same
     * keys: recognized keys map to the knowledge columns, everything else is
     * stored in the attributes JSON bag.
     */
    @NotEmpty
    private List<Map<String, String>> entries;

    /** skip (default) or overwrite when an entry name already exists. */
    private String duplicate = "skip";

    /** false aborts the whole import when any entry is invalid; true (default) skips them. */
    private boolean continueOnError = true;
}
