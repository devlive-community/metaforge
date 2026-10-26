package org.devlive.metaforge.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils
{
    private DateUtils()
    {}

    public static Long now()
    {
        return Instant.now()
                .toEpochMilli();
    }

    public static String formatYMD()
    {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return now.format(formatter);
    }

    public static String formatYMDHMS()
    {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return now.format(formatter);
    }

    public static String formatYMDHMSWithInterval()
    {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}
