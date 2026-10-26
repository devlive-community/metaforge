package org.devlive.metaforge.common.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class JwtResponse
{
    private String type = "Bearer";
    private String token;
    private String code;
    private String username;
    private List<String> roles;
    private String avatar;

    public JwtResponse(String accessToken, String code, String username, List<String> roles, String avatar)
    {
        this.token = accessToken;
        this.code = code;
        this.username = username;
        // Use an immutable list to ensure that the roles property cannot be modified externally
        this.roles = Collections.unmodifiableList(roles);
        this.avatar = avatar;
    }
}
