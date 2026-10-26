package org.devlive.metaforge.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

@Slf4j
public class ResourceUtils
{
    private ResourceUtils()
    {}

    public static String loadResourceToString(String resource, boolean absolute)
    {
        try {
            if (absolute) {
                return IOUtils.toString(Files.newInputStream(new File(resource).toPath()), Charset.defaultCharset());
            }
            else {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                InputStream inputStream = loader.getResourceAsStream(resource);
                return IOUtils.toString(inputStream, Charset.defaultCharset());
            }
        }
        catch (IOException e) {
            log.error("Loading file error {}", resource);
        }
        return null;
    }

    public static boolean checkExists(String resource)
    {
        return !ObjectUtils.isEmpty(loadResourceToString(resource, false));
    }
}
