package org.devlive.metaforge.fs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.InputStream;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FsResponse
{
    private String origin;
    private String remote;
    private String message;
    private InputStream context;
    private boolean successful;
}
