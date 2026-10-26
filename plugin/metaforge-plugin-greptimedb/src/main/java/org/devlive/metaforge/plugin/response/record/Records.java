package org.devlive.metaforge.plugin.response.record;

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
@SuppressFBWarnings(value = {"PA_PUBLIC_PRIMITIVE_ATTRIBUTE"})
public class Records
{
    public Schema schema;
    public List<List<Object>> rows;
}
