package org.devlive.metaforge.spi.parser;

import org.devlive.metaforge.sql.statement.SQLStatement;

public interface Parser
{
    SQLStatement getStatement();

    String getExecuteContext();
}
