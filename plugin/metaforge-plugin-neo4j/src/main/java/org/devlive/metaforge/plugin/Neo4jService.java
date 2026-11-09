package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

/**
 * Neo4j is browsed like other sources: the database the source connects to, its node labels as
 * tables and their properties as columns, read from the Neo4j JDBC driver's metadata.
 */
public class Neo4jService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "neo4j:bolt";
    }

    @Override
    public String driver()
    {
        return "org.neo4j.jdbc.Driver";
    }

    /** The driver runs Cypher, so the server version comes from dbms.components(). */
    @Override
    public String validator()
    {
        return "CALL dbms.components() YIELD versions RETURN versions[0] AS version";
    }

    /** The driver reports no schemas; a connection reads the one database it was opened on. */
    @Override
    public Response getDatabases(RuntimeConfigure configure)
    {
        return execute(configure, "CALL db.info() YIELD name RETURN name AS object_name, 'DATABASE' AS object_type, '' AS object_comment");
    }

    // Labels and properties carry no schema in the driver's metadata, so they are read without one.
    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        return metadataTables(configure, null);
    }

    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        return metadataColumns(configure, null, table);
    }
}
