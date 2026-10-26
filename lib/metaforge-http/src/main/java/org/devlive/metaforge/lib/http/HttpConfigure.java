package org.devlive.metaforge.lib.http;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import okhttp3.MediaType;

import java.util.Map;

@Data
@ToString
@EqualsAndHashCode
@SuppressFBWarnings(value = {"EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC", "EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class HttpConfigure
{
    private String protocol = "http";
    private String host;
    private int port;
    private String path;
    private HttpMethod method;
    private Integer retry = 0;
    private Boolean autoConnected = Boolean.FALSE;
    private Map<String, String> params;
    private String body;
    private MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
}
