package org.devlive.metaforge.common.utils;

public class SqlCheckerUtils
{
    private SqlCheckerUtils()
    {
    }

    /**
     * Check whether the input SQL statement contains paging configurations
     * <p>
     * SQL input @param sql
     *
     * @return true indicates that paging configuration is included, and false indicates that paging configuration is not included
     */
    public static boolean isPagedSql(String sql)
    {
        // Check whether the SQL statement contains the limit keyword or the rownum pseudo column
        if (sql.toLowerCase().contains("limit") || sql.toLowerCase().contains("rownum")) {
            // If a pagination configuration is included, you need to further determine whether it is in the comments
            String[] lines = sql.split("\\n");
            for (String line : lines) {
                // Remove the space before the comment symbol "--".
                line = line.replaceAll("\\s*--.*", "");
                if (line.toLowerCase().contains("limit") || line.toLowerCase().contains("rownum")) {
                    // If the paging configuration is still included, the description is not in the comments
                    return true;
                }
            }
        }
        return false;
    }
}
