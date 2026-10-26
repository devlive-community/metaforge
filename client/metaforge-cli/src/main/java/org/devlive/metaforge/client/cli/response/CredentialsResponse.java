package org.devlive.metaforge.client.cli.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsResponse
{
    private boolean connect;
    private String message;
    private Object data;
}
