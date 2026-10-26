package org.devlive.metaforge.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class CrateDBHTTPResponse
{
    @JsonProperty(value = "cols")
    private List<String> headers;

    @JsonProperty(value = "col_types")
    private List<String> types;

    @JsonProperty(value = "rows")
    private List<Object> columns;

    @JsonProperty(value = "error")
    private CrateDBHTTPResponseError error;
}
