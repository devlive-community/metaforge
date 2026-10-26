package org.devlive.metaforge.spi.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class Response
{
    private List<String> headers;
    private List<String> types;
    private List<Object> columns;
    private Boolean isConnected = Boolean.FALSE;
    private Boolean isSuccessful = Boolean.FALSE;
    private String message;
    private Time connection;
    private Time processor;
    private String content;
    private Pagination pagination;
}
