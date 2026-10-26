package org.devlive.metaforge.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiModel
{
    private String type = "ChatGPT";
    private String host;
    private String token;
    private long timeout;
    // Number of times a context needs to be associated
    private int contentCount = 5;
}
