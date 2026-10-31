package org.devlive.metaforge.driver.sql.base;

/**
 * Every {@link java.sql.DatabaseMetaData} method refuses until a driver overrides it:
 * with {@link java.sql.SQLFeatureNotSupportedException} where the interface allows it. Generated
 * from the JDK interface; do not edit by hand.
 */
public abstract class BaseDatabaseMetaData
        implements java.sql.DatabaseMetaData
{
    @Override
    public boolean allProceduresAreCallable()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.allProceduresAreCallable");
    }

    @Override
    public boolean allTablesAreSelectable()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.allTablesAreSelectable");
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.autoCommitFailureClosesAllResultSets");
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.dataDefinitionCausesTransactionCommit");
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.dataDefinitionIgnoredInTransactions");
    }

    @Override
    public boolean deletesAreDetected(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.deletesAreDetected");
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.doesMaxRowSizeIncludeBlobs");
    }

    @Override
    public boolean generatedKeyAlwaysReturned()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.generatedKeyAlwaysReturned");
    }

    @Override
    public java.sql.ResultSet getAttributes(String arg0, String arg1, String arg2, String arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getAttributes");
    }

    @Override
    public java.sql.ResultSet getBestRowIdentifier(String arg0, String arg1, String arg2, int arg3, boolean arg4)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getBestRowIdentifier");
    }

    @Override
    public String getCatalogSeparator()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getCatalogSeparator");
    }

    @Override
    public String getCatalogTerm()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getCatalogTerm");
    }

    @Override
    public java.sql.ResultSet getCatalogs()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getCatalogs");
    }

    @Override
    public java.sql.ResultSet getClientInfoProperties()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getClientInfoProperties");
    }

    @Override
    public java.sql.ResultSet getColumnPrivileges(String arg0, String arg1, String arg2, String arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getColumnPrivileges");
    }

    @Override
    public java.sql.ResultSet getColumns(String arg0, String arg1, String arg2, String arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getColumns");
    }

    @Override
    public java.sql.Connection getConnection()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getConnection");
    }

    @Override
    public java.sql.ResultSet getCrossReference(String arg0, String arg1, String arg2, String arg3, String arg4, String arg5)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getCrossReference");
    }

    @Override
    public int getDatabaseMajorVersion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getDatabaseMajorVersion");
    }

    @Override
    public int getDatabaseMinorVersion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getDatabaseMinorVersion");
    }

    @Override
    public String getDatabaseProductName()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getDatabaseProductName");
    }

    @Override
    public String getDatabaseProductVersion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getDatabaseProductVersion");
    }

    @Override
    public int getDefaultTransactionIsolation()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getDefaultTransactionIsolation");
    }

    @Override
    public int getDriverMajorVersion()
    {
        throw new UnsupportedOperationException("DatabaseMetaData.getDriverMajorVersion");
    }

    @Override
    public int getDriverMinorVersion()
    {
        throw new UnsupportedOperationException("DatabaseMetaData.getDriverMinorVersion");
    }

    @Override
    public String getDriverName()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getDriverName");
    }

    @Override
    public String getDriverVersion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getDriverVersion");
    }

    @Override
    public java.sql.ResultSet getExportedKeys(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getExportedKeys");
    }

    @Override
    public String getExtraNameCharacters()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getExtraNameCharacters");
    }

    @Override
    public java.sql.ResultSet getFunctionColumns(String arg0, String arg1, String arg2, String arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getFunctionColumns");
    }

    @Override
    public java.sql.ResultSet getFunctions(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getFunctions");
    }

    @Override
    public String getIdentifierQuoteString()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getIdentifierQuoteString");
    }

    @Override
    public java.sql.ResultSet getImportedKeys(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getImportedKeys");
    }

    @Override
    public java.sql.ResultSet getIndexInfo(String arg0, String arg1, String arg2, boolean arg3, boolean arg4)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getIndexInfo");
    }

    @Override
    public int getJDBCMajorVersion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getJDBCMajorVersion");
    }

    @Override
    public int getJDBCMinorVersion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getJDBCMinorVersion");
    }

    @Override
    public int getMaxBinaryLiteralLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxBinaryLiteralLength");
    }

    @Override
    public int getMaxCatalogNameLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxCatalogNameLength");
    }

    @Override
    public int getMaxCharLiteralLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxCharLiteralLength");
    }

    @Override
    public int getMaxColumnNameLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxColumnNameLength");
    }

    @Override
    public int getMaxColumnsInGroupBy()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxColumnsInGroupBy");
    }

    @Override
    public int getMaxColumnsInIndex()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxColumnsInIndex");
    }

    @Override
    public int getMaxColumnsInOrderBy()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxColumnsInOrderBy");
    }

    @Override
    public int getMaxColumnsInSelect()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxColumnsInSelect");
    }

    @Override
    public int getMaxColumnsInTable()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxColumnsInTable");
    }

    @Override
    public int getMaxConnections()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxConnections");
    }

    @Override
    public int getMaxCursorNameLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxCursorNameLength");
    }

    @Override
    public int getMaxIndexLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxIndexLength");
    }

    @Override
    public int getMaxProcedureNameLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxProcedureNameLength");
    }

    @Override
    public int getMaxRowSize()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxRowSize");
    }

    @Override
    public int getMaxSchemaNameLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxSchemaNameLength");
    }

    @Override
    public int getMaxStatementLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxStatementLength");
    }

    @Override
    public int getMaxStatements()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxStatements");
    }

    @Override
    public int getMaxTableNameLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxTableNameLength");
    }

    @Override
    public int getMaxTablesInSelect()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxTablesInSelect");
    }

    @Override
    public int getMaxUserNameLength()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getMaxUserNameLength");
    }

    @Override
    public String getNumericFunctions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getNumericFunctions");
    }

    @Override
    public java.sql.ResultSet getPrimaryKeys(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getPrimaryKeys");
    }

    @Override
    public java.sql.ResultSet getProcedureColumns(String arg0, String arg1, String arg2, String arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getProcedureColumns");
    }

    @Override
    public String getProcedureTerm()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getProcedureTerm");
    }

    @Override
    public java.sql.ResultSet getProcedures(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getProcedures");
    }

    @Override
    public java.sql.ResultSet getPseudoColumns(String arg0, String arg1, String arg2, String arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getPseudoColumns");
    }

    @Override
    public int getResultSetHoldability()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getResultSetHoldability");
    }

    @Override
    public java.sql.RowIdLifetime getRowIdLifetime()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getRowIdLifetime");
    }

    @Override
    public String getSQLKeywords()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSQLKeywords");
    }

    @Override
    public int getSQLStateType()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSQLStateType");
    }

    @Override
    public String getSchemaTerm()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSchemaTerm");
    }

    @Override
    public java.sql.ResultSet getSchemas()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSchemas");
    }

    @Override
    public java.sql.ResultSet getSchemas(String arg0, String arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSchemas");
    }

    @Override
    public String getSearchStringEscape()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSearchStringEscape");
    }

    @Override
    public String getStringFunctions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getStringFunctions");
    }

    @Override
    public java.sql.ResultSet getSuperTables(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSuperTables");
    }

    @Override
    public java.sql.ResultSet getSuperTypes(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSuperTypes");
    }

    @Override
    public String getSystemFunctions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getSystemFunctions");
    }

    @Override
    public java.sql.ResultSet getTablePrivileges(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getTablePrivileges");
    }

    @Override
    public java.sql.ResultSet getTableTypes()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getTableTypes");
    }

    @Override
    public java.sql.ResultSet getTables(String arg0, String arg1, String arg2, String[] arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getTables");
    }

    @Override
    public String getTimeDateFunctions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getTimeDateFunctions");
    }

    @Override
    public java.sql.ResultSet getTypeInfo()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getTypeInfo");
    }

    @Override
    public java.sql.ResultSet getUDTs(String arg0, String arg1, String arg2, int[] arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getUDTs");
    }

    @Override
    public String getURL()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getURL");
    }

    @Override
    public String getUserName()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getUserName");
    }

    @Override
    public java.sql.ResultSet getVersionColumns(String arg0, String arg1, String arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.getVersionColumns");
    }

    @Override
    public boolean insertsAreDetected(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.insertsAreDetected");
    }

    @Override
    public boolean isCatalogAtStart()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.isCatalogAtStart");
    }

    @Override
    public boolean isReadOnly()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.isReadOnly");
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.isWrapperFor");
    }

    @Override
    public boolean locatorsUpdateCopy()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.locatorsUpdateCopy");
    }

    @Override
    public boolean nullPlusNonNullIsNull()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.nullPlusNonNullIsNull");
    }

    @Override
    public boolean nullsAreSortedAtEnd()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.nullsAreSortedAtEnd");
    }

    @Override
    public boolean nullsAreSortedAtStart()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.nullsAreSortedAtStart");
    }

    @Override
    public boolean nullsAreSortedHigh()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.nullsAreSortedHigh");
    }

    @Override
    public boolean nullsAreSortedLow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.nullsAreSortedLow");
    }

    @Override
    public boolean othersDeletesAreVisible(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.othersDeletesAreVisible");
    }

    @Override
    public boolean othersInsertsAreVisible(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.othersInsertsAreVisible");
    }

    @Override
    public boolean othersUpdatesAreVisible(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.othersUpdatesAreVisible");
    }

    @Override
    public boolean ownDeletesAreVisible(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.ownDeletesAreVisible");
    }

    @Override
    public boolean ownInsertsAreVisible(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.ownInsertsAreVisible");
    }

    @Override
    public boolean ownUpdatesAreVisible(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.ownUpdatesAreVisible");
    }

    @Override
    public boolean storesLowerCaseIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.storesLowerCaseIdentifiers");
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.storesLowerCaseQuotedIdentifiers");
    }

    @Override
    public boolean storesMixedCaseIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.storesMixedCaseIdentifiers");
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.storesMixedCaseQuotedIdentifiers");
    }

    @Override
    public boolean storesUpperCaseIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.storesUpperCaseIdentifiers");
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.storesUpperCaseQuotedIdentifiers");
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsANSI92EntryLevelSQL");
    }

    @Override
    public boolean supportsANSI92FullSQL()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsANSI92FullSQL");
    }

    @Override
    public boolean supportsANSI92IntermediateSQL()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsANSI92IntermediateSQL");
    }

    @Override
    public boolean supportsAlterTableWithAddColumn()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsAlterTableWithAddColumn");
    }

    @Override
    public boolean supportsAlterTableWithDropColumn()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsAlterTableWithDropColumn");
    }

    @Override
    public boolean supportsBatchUpdates()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsBatchUpdates");
    }

    @Override
    public boolean supportsCatalogsInDataManipulation()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsCatalogsInDataManipulation");
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsCatalogsInIndexDefinitions");
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsCatalogsInPrivilegeDefinitions");
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsCatalogsInProcedureCalls");
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsCatalogsInTableDefinitions");
    }

    @Override
    public boolean supportsColumnAliasing()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsColumnAliasing");
    }

    @Override
    public boolean supportsConvert()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsConvert");
    }

    @Override
    public boolean supportsConvert(int arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsConvert");
    }

    @Override
    public boolean supportsCoreSQLGrammar()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsCoreSQLGrammar");
    }

    @Override
    public boolean supportsCorrelatedSubqueries()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsCorrelatedSubqueries");
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsDataDefinitionAndDataManipulationTransactions");
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsDataManipulationTransactionsOnly");
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsDifferentTableCorrelationNames");
    }

    @Override
    public boolean supportsExpressionsInOrderBy()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsExpressionsInOrderBy");
    }

    @Override
    public boolean supportsExtendedSQLGrammar()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsExtendedSQLGrammar");
    }

    @Override
    public boolean supportsFullOuterJoins()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsFullOuterJoins");
    }

    @Override
    public boolean supportsGetGeneratedKeys()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsGetGeneratedKeys");
    }

    @Override
    public boolean supportsGroupBy()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsGroupBy");
    }

    @Override
    public boolean supportsGroupByBeyondSelect()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsGroupByBeyondSelect");
    }

    @Override
    public boolean supportsGroupByUnrelated()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsGroupByUnrelated");
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsIntegrityEnhancementFacility");
    }

    @Override
    public boolean supportsLikeEscapeClause()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsLikeEscapeClause");
    }

    @Override
    public boolean supportsLimitedOuterJoins()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsLimitedOuterJoins");
    }

    @Override
    public boolean supportsMinimumSQLGrammar()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsMinimumSQLGrammar");
    }

    @Override
    public boolean supportsMixedCaseIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsMixedCaseIdentifiers");
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsMixedCaseQuotedIdentifiers");
    }

    @Override
    public boolean supportsMultipleOpenResults()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsMultipleOpenResults");
    }

    @Override
    public boolean supportsMultipleResultSets()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsMultipleResultSets");
    }

    @Override
    public boolean supportsMultipleTransactions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsMultipleTransactions");
    }

    @Override
    public boolean supportsNamedParameters()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsNamedParameters");
    }

    @Override
    public boolean supportsNonNullableColumns()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsNonNullableColumns");
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsOpenCursorsAcrossCommit");
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsOpenCursorsAcrossRollback");
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsOpenStatementsAcrossCommit");
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsOpenStatementsAcrossRollback");
    }

    @Override
    public boolean supportsOrderByUnrelated()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsOrderByUnrelated");
    }

    @Override
    public boolean supportsOuterJoins()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsOuterJoins");
    }

    @Override
    public boolean supportsPositionedDelete()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsPositionedDelete");
    }

    @Override
    public boolean supportsPositionedUpdate()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsPositionedUpdate");
    }

    @Override
    public boolean supportsResultSetConcurrency(int arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsResultSetConcurrency");
    }

    @Override
    public boolean supportsResultSetHoldability(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsResultSetHoldability");
    }

    @Override
    public boolean supportsResultSetType(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsResultSetType");
    }

    @Override
    public boolean supportsSavepoints()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSavepoints");
    }

    @Override
    public boolean supportsSchemasInDataManipulation()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSchemasInDataManipulation");
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSchemasInIndexDefinitions");
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSchemasInPrivilegeDefinitions");
    }

    @Override
    public boolean supportsSchemasInProcedureCalls()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSchemasInProcedureCalls");
    }

    @Override
    public boolean supportsSchemasInTableDefinitions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSchemasInTableDefinitions");
    }

    @Override
    public boolean supportsSelectForUpdate()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSelectForUpdate");
    }

    @Override
    public boolean supportsStatementPooling()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsStatementPooling");
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsStoredFunctionsUsingCallSyntax");
    }

    @Override
    public boolean supportsStoredProcedures()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsStoredProcedures");
    }

    @Override
    public boolean supportsSubqueriesInComparisons()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSubqueriesInComparisons");
    }

    @Override
    public boolean supportsSubqueriesInExists()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSubqueriesInExists");
    }

    @Override
    public boolean supportsSubqueriesInIns()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSubqueriesInIns");
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsSubqueriesInQuantifieds");
    }

    @Override
    public boolean supportsTableCorrelationNames()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsTableCorrelationNames");
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsTransactionIsolationLevel");
    }

    @Override
    public boolean supportsTransactions()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsTransactions");
    }

    @Override
    public boolean supportsUnion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsUnion");
    }

    @Override
    public boolean supportsUnionAll()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.supportsUnionAll");
    }

    @Override
    public <T> T unwrap(Class<T> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.unwrap");
    }

    @Override
    public boolean updatesAreDetected(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.updatesAreDetected");
    }

    @Override
    public boolean usesLocalFilePerTable()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.usesLocalFilePerTable");
    }

    @Override
    public boolean usesLocalFiles()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("DatabaseMetaData.usesLocalFiles");
    }
}
