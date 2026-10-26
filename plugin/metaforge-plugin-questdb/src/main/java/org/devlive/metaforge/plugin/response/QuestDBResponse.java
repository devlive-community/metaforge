package org.devlive.metaforge.plugin.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class QuestDBResponse
{
    private String error;
    private String query;
    private List<Schema> columns;
    private List<List<Object>> dataset;
}
