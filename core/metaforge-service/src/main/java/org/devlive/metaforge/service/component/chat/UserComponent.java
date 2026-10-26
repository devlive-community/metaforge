package org.devlive.metaforge.service.component.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class UserComponent
{
    @JsonProperty(value = "_id")
    private Long id;

    @JsonProperty(value = "username")
    private String username;

    @JsonProperty(value = "avatar")
    private String avatar;
}
