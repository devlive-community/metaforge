package org.devlive.metaforge.plugin.response.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = {"PA_PUBLIC_PRIMITIVE_ATTRIBUTE"})
public class Schema
{
    @JsonProperty(value = "column_schemas")
    public ArrayList<ColumnSchema> columnSchemas;
}
