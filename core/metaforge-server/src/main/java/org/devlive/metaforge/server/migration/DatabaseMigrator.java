package org.devlive.metaforge.server.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.core.H2Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.structure.core.Table;

import java.sql.Connection;

/**
 * Brings a metadata database to the latest changelog version: creates the schema on an
 * empty database, upgrades installations made by older releases and applies every
 * pending changeset. Safe to run on every boot.
 */
public final class DatabaseMigrator
{
    public static final String CHANGELOG = "db/changelog/db.changelog-master.xml";
    public static final String CHANGELOG_TABLE = "metaforge_changelog";
    public static final String CHANGELOG_LOCK_TABLE = "metaforge_changelog_lock";

    private DatabaseMigrator()
    {
    }

    /**
     * Migrates through the given connection; the caller keeps ownership of the connection.
     */
    public static void migrate(Connection connection)
            throws Exception
    {
        JdbcConnection jdbcConnection = new JdbcConnection(connection);
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
        if (database instanceof H2Database && connection.getMetaData().storesLowerCaseIdentifiers()) {
            database = new LowerCaseH2Database();
            database.setConnection(jdbcConnection);
        }
        // Liquibase looks the history tables up verbatim, so fold the names the way the database does.
        database.setDatabaseChangeLogTableName(database.correctObjectName(CHANGELOG_TABLE, Table.class));
        database.setDatabaseChangeLogLockTableName(database.correctObjectName(CHANGELOG_LOCK_TABLE, Table.class));
        Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(DatabaseMigrator.class.getClassLoader()), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }
}
