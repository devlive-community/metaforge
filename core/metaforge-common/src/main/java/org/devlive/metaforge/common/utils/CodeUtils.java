package org.devlive.metaforge.common.utils;

import java.util.UUID;

public class CodeUtils
{
    private CodeUtils() {}

    /**
     * Generate code
     *
     * @param dash if dash is true, the code will be generated with dash
     * @return code
     */
    public static String generateCode(boolean dash)
    {
        String code = UUID.randomUUID().toString();
        if (dash) {
            return code;
        }
        return code.replace("-", "");
    }
}
