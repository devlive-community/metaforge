package org.devlive.metaforge.server.migration;

import liquibase.database.core.H2Database;

/**
 * H2 opened with DATABASE_TO_LOWER (the embedded store) folds unquoted names to lower case,
 * while Liquibase's H2 support assumes upper case and then misses every existing table.
 */
public class LowerCaseH2Database
        extends H2Database
{
    public LowerCaseH2Database()
    {
        unquotedObjectsAreUppercased = Boolean.FALSE;
    }
}
