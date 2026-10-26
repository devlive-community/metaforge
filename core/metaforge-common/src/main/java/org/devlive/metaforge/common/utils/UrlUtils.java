package org.devlive.metaforge.common.utils;

import org.apache.commons.lang3.StringUtils;

public class UrlUtils
{
    private UrlUtils()
    {
    }

    /**
     * Replaces multiple consecutive slashes in the given URL with a single slash.
     *
     * @param url the URL to fix
     * @return the fixed URL with single slashes
     */
    public static String fixUrl(String url)
    {
        if (StringUtils.isNotEmpty(url)) {
            return url.replaceAll("/+", "/");
        }
        return null;
    }
}
