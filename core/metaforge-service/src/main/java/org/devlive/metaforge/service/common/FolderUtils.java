package org.devlive.metaforge.service.common;

import org.devlive.metaforge.common.utils.DateUtils;

import java.io.File;

public class FolderUtils
{
    private FolderUtils()
    {}

    /**
     * Generates a work home directory path based on the given parameters.<br />
     * format: ${DATA_HONE}/${YMD}/${USER_NAME}/${TYPE}
     *
     * @param dataHome the base directory where the work home will be created
     * @param userName the name of the user
     * @param type the type of work home
     * @return the generated work home directory path
     */
    public static String getWorkHome(String dataHome, String userName, String type)
    {
        return String.join(File.separator, dataHome, userName, DateUtils.formatYMD(), type);
    }
}
