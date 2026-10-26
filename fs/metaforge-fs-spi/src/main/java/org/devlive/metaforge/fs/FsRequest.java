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
public class FsRequest
{
    private String access;
    private String secret;
    private String endpoint;
    private String bucket;
    @Deprecated
    private String localPath;
    private InputStream stream;
    private String fileName;
}
