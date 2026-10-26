package org.devlive.metaforge.service.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SharedSourceBody
        extends BaseUserBody
{
    @JsonProperty(value = "public")
    private Boolean publish;
    private Long sourceId;
}
