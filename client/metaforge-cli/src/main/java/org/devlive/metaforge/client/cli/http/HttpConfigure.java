package org.devlive.metaforge.client.cli.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class HttpConfigure
{
    private String host;
    private int port;
    private String username;
    private String password;
    private String token;
    private HttpMethod method = HttpMethod.GET;
    private HttpProtocol protocol = HttpProtocol.http;
    private String url;
    private Long sourceId;
}
