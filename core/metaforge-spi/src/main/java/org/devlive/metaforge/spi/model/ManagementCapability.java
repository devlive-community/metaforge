package org.devlive.metaforge.spi.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every management capability a plugin may declare, with the parts of the source management
 * pages it controls. A page, tab, dialog, drawer, side panel or button of those pages shows only
 * when the source's plugin declares its capability, and the server refuses the matching request
 * otherwise. A new management feature adds its capability here first and guards every surface it
 * brings with it.
 */
public enum ManagementCapability
{
    QUERY_EXECUTE("query:execute", "Run SQL in the query console and the SQL tab"),
    NAMESPACE_LIST("namespace:list", "Namespace list of the native browser (Redis databases, buckets)"),
    KEY_LIST("key:list", "Key list of the native browser"),
    KEY_READ("key:read", "Key detail panel"),
    OBJECT_LIST("object:list", "Object list of the storage browser"),
    OBJECT_READ("object:read", "Object detail panel"),
    OBJECT_DOWNLOAD("object:download", "Object download button"),
    DATABASE_LIST("database:list", "Database selector of the workbench header and the database list of the side tree"),
    DATABASE_INFO("database:info", "Database overview page"),
    DATABASE_ALTER("database:alter", "Edit database properties button and drawer"),
    ENGINE_LIST("engine:list", "Engine select of the create table dialog and engine choices of the table properties drawer"),
    DATATYPE_LIST("datatype:list", "Type selects of the create table dialog and the column editors (free text otherwise)"),
    TABLE_LIST("table:list", "Table tree of the side panel and its search"),
    TABLE_INFO("table:info", "Table header facts and the table properties tab"),
    TABLE_READ("table:read", "Data preview tab, data compare and the sample data DDL"),
    TABLE_STATEMENT("table:statement", "DDL tab, the DDL quick menu and structure sync SQL"),
    TABLE_CREATE("table:create", "New table entries (side tree, more actions, database overview) and new tables of an import"),
    TABLE_ALTER("table:alter", "Edit table properties button and drawer"),
    TABLE_DROP("table:drop", "Drop table entries and dialog"),
    TABLE_TRUNCATE("table:truncate", "Truncate table entries and dialog, and empty-then-import"),
    TABLE_EXPORT("table:export", "Export data entries, dialog and the data export page"),
    TABLE_AUTO_INCREMENT("table:auto-increment", "Auto increment start of the create table dialog and the auto increment switches of the column editors"),
    TABLE_OPTIONS("table:options", "Charset, collation and row format of the create table dialog"),
    COLUMN_READ("column:read", "Column structure, structure compare and the data compare entry"),
    COLUMN_CREATE("column:create", "Add and insert column buttons and the column drawer"),
    COLUMN_MODIFY("column:modify", "Edit column button and drawer"),
    COLUMN_DROP("column:drop", "Drop column button and dialog"),
    COLUMN_REORDER("column:reorder", "Column drag sorting and the order dialog"),
    INDEX_READ("index:read", "Index list"),
    INDEX_CREATE("index:create", "New index button and drawer, and the index step of the create table dialog"),
    INDEX_DROP("index:drop", "Drop index button and dialog"),
    FOREIGN_KEY_READ("foreign-key:read", "Foreign key list"),
    FOREIGN_KEY_CREATE("foreign-key:create", "New foreign key button and drawer, and the foreign key tab of the create table dialog"),
    FOREIGN_KEY_DROP("foreign-key:drop", "Drop foreign key button and dialog"),
    TRIGGER_READ("trigger:read", "Trigger list"),
    TRIGGER_CREATE("trigger:create", "New and copy trigger buttons and the trigger drawer"),
    TRIGGER_DROP("trigger:drop", "Drop trigger button and dialog"),
    DATA_INSERT("data:insert", "Add row in the data preview and the data import page"),
    DATA_MODIFY("data:modify", "Edit row in the data preview"),
    DATA_DELETE("data:delete", "Delete rows in the data preview"),
    TAB_STRUCTURE("tab:structure", "Column structure tab"),
    TAB_DATA("tab:data", "Data preview tab"),
    TAB_SQL("tab:sql", "SQL tab"),
    TAB_INDEXES("tab:indexes", "Indexes tab"),
    TAB_FOREIGN_KEYS("tab:foreignkeys", "Foreign keys tab"),
    TAB_TRIGGERS("tab:triggers", "Triggers tab"),
    TAB_INFO("tab:info", "Table properties tab"),
    TAB_STATEMENT("tab:statement", "DDL tab"),
    TAB_ER_DIAGRAM("tab:er-diagram", "Table ER tab and the database ER page"),
    TAB_KEYS("tab:keys", "Key browser tab"),
    TAB_OBJECTS("tab:objects", "Object browser tab");

    /** Kinds a plugin may declare; they choose the workspace layout. */
    public static final Set<String> KINDS = Set.of("relational", "key-value", "document", "storage", "graph", "unknown");

    private final String key;
    private final String surfaces;

    ManagementCapability(String key, String surfaces)
    {
        this.key = key;
        this.surfaces = surfaces;
    }

    public String getKey()
    {
        return key;
    }

    public String getSurfaces()
    {
        return surfaces;
    }

    public static Optional<ManagementCapability> of(String key)
    {
        return Arrays.stream(values()).filter(value -> value.key.equals(key)).findFirst();
    }

    /** Every capability name, in declaration order. */
    public static Set<String> keys()
    {
        return Arrays.stream(values()).map(ManagementCapability::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
