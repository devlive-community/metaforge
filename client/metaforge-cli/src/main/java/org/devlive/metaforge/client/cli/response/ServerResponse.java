package org.devlive.metaforge.client.cli.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse
{
    private boolean status;
    private int code;
    private String message;
    private Object data;
}
