package org.devlive.metaforge.sql

import java.io.IOException
import java.util.*

abstract class AbstractSql<T> {
    companion object {
        @JvmStatic
        val AND = ") \nAND ("

        @JvmStatic
        val OR = ") \nOR ("
    }

    private val sql = SQLStatement()

    abstract fun getSelf(): T

    fun UPDATE(table: String?): T {
        sql().statementType = StatementType.UPDATE
        sql().tables.add(table)
        return getSelf()
    }

    fun SET(sets: String?): T {
        sql().sets.add(sets)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun SET(vararg sets: String?): T {
        sql().sets.addAll(Arrays.asList(*sets))
        return getSelf()
    }

    fun INSERT_INTO(tableName: String?): T {
        sql().statementType = StatementType.INSERT
        sql().tables.add(tableName)
        return getSelf()
    }

    fun VALUES(columns: String?, values: String?): T {
        INTO_COLUMNS(columns)
        INTO_VALUES(values)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun INTO_COLUMNS(vararg columns: String?): T {
        sql().columns.addAll(Arrays.asList(*columns))
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun INTO_VALUES(vararg values: String?): T {
        val list = sql().valuesList[sql().valuesList.size - 1]
        for (value in values) {
            list.add(value)
        }
        return getSelf()
    }

    fun SELECT(columns: String?): T {
        sql().statementType = StatementType.SELECT
        sql().select.add(columns)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun SELECT(vararg columns: String?): T {
        sql().statementType = StatementType.SELECT
        sql().select.addAll(Arrays.asList(*columns))
        return getSelf()
    }

    fun SELECT_DISTINCT(columns: String?): T {
        sql().distinct = true
        SELECT(columns)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun SELECT_DISTINCT(vararg columns: String?): T {
        sql().distinct = true
        SELECT(*columns)
        return getSelf()
    }

    fun DELETE_FROM(table: String?): T {
        sql().statementType = StatementType.DELETE
        sql().tables.add(table)
        return getSelf()
    }

    fun TRUNCATE(table: String?): T {
        sql().statementType = StatementType.TRUNCATE
        sql().tables.add(table)
        return getSelf()
    }

    fun DROP(table: String?): T {
        sql().statementType = StatementType.DROP
        sql().tables.add(table)
        return getSelf()
    }

    fun CREATE_TABLE(table: String?): T {
        sql().statementType = StatementType.CREATE_TABLE
        sql().tables.add(table)
        return getSelf()
    }

    fun MODIFY_LIFECYCLE(table: String?): T {
        sql().statementType = StatementType.MODIFY_LIFECYCLE
        sql().tables.add(table)
        return getSelf()
    }

    fun LIFECYCLE(lifecycle: String?): T {
        sql().lifecycle = lifecycle
        return getSelf()
    }

    fun ADD_LIFECYCLE(lifecycle: String?): T {
        sql().lifecycle = lifecycle
        return getSelf()
    }

    fun CREATE_COLUMN(table: String?): T {
        sql().statementType = StatementType.CREATE_COLUMN
        sql().tables.add(table)
        return getSelf()
    }

    fun DROP_COLUMN(table: String?): T {
        sql().statementType = StatementType.DROP_COLUMN
        sql().tables.add(table)
        return getSelf()
    }

    fun MODIFY_COLUMN(table: String?): T {
        sql().statementType = StatementType.MODIFY_COLUMN
        sql().tables.add(table)
        return getSelf()
    }

    fun COLUMNS(columns: List<String>): T {
        sql().columns.addAll(columns.map { item -> "\t$item" })
        return getSelf()
    }

    fun FORMAT_ENGINE(engine: EngineType?): T {
        sql().formatEngine = engine
        return getSelf()
    }

    fun ENGINE(engine: String?): T {
        sql().engine = engine
        return getSelf()
    }

    fun ORDER_BY_KEY(columns: List<String>): T {
        sql().orderByKey.addAll(columns.map { item -> "\t$item" })
        return getSelf()
    }

    fun PARTITION_BY_KEY(columns: List<String>): T {
        sql().partitionByKey.addAll(columns.map { item -> "\t$item" })
        return getSelf()
    }

    fun PRIMARY_KEY(columns: List<String>): T {
        sql().primaryKey.addAll(columns.map { item -> "\t$item" })
        return getSelf()
    }

    fun SAMPING_KEY(columns: List<String>): T {
        sql().samplingKey.addAll(columns.map { item -> "\t$item" })
        return getSelf()
    }

    fun FROM(table: String?): T {
        sql().tables.add(table)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun FROM(vararg tables: String?): T {
        sql().tables.addAll(Arrays.asList(*tables))
        return getSelf()
    }

    fun JOIN(join: String?): T {
        sql().join.add(join)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun JOIN(vararg joins: String?): T {
        sql().join.addAll(Arrays.asList(*joins))
        return getSelf()
    }

    fun INNER_JOIN(join: String?): T {
        sql().innerJoin.add(join)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun INNER_JOIN(vararg joins: String?): T {
        sql().innerJoin.addAll(Arrays.asList(*joins))
        return getSelf()
    }

    fun LEFT_OUTER_JOIN(join: String?): T {
        sql().leftOuterJoin.add(join)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun LEFT_OUTER_JOIN(vararg joins: String?): T {
        sql().leftOuterJoin.addAll(Arrays.asList(*joins))
        return getSelf()
    }

    fun RIGHT_OUTER_JOIN(join: String?): T {
        sql().rightOuterJoin.add(join)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun RIGHT_OUTER_JOIN(vararg joins: String?): T {
        sql().rightOuterJoin.addAll(Arrays.asList(*joins))
        return getSelf()
    }

    fun OUTER_JOIN(join: String?): T {
        sql().outerJoin.add(join)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun OUTER_JOIN(vararg joins: String?): T {
        sql().outerJoin.addAll(Arrays.asList(*joins))
        return getSelf()
    }

    fun WHERE(conditions: String?): T {
        sql().where.add(conditions)
        sql().lastList = sql().where
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun WHERE(vararg conditions: String?): T {
        sql().where.addAll(Arrays.asList(*conditions))
        sql().lastList = sql().where
        return getSelf()
    }

    fun OR(): T {
        sql().lastList.add(OR)
        return getSelf()
    }

    fun AND(): T {
        sql().lastList.add(AND)
        return getSelf()
    }

    fun GROUP_BY(columns: String?): T {
        sql().groupBy.add(columns)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun GROUP_BY(vararg columns: String?): T {
        sql().groupBy.addAll(Arrays.asList(*columns))
        return getSelf()
    }

    fun HAVING(conditions: String?): T {
        sql().having.add(conditions)
        sql().lastList = sql().having
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun HAVING(vararg conditions: String?): T {
        sql().having.addAll(Arrays.asList(*conditions))
        sql().lastList = sql().having
        return getSelf()
    }

    fun ORDER_BY(columns: String?): T {
        sql().orderBy.add(columns)
        return getSelf()
    }

    /**
     * @since 3.4.2
     */
    fun ORDER_BY(vararg columns: String?): T {
        sql().orderBy.addAll(Arrays.asList(*columns))
        return getSelf()
    }

    /**
     * Set the limit variable string(e.g. `"#{limit}"`).
     *
     * @param variable a limit variable string
     * @return a self instance
     * @see .OFFSET
     * @since 3.5.2
     */
    fun LIMIT(variable: String?): T {
        sql().limit = variable
        sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.OFFSET_LIMIT
        return getSelf()
    }

    /**
     * Set the limit value.
     *
     * @param value an offset value
     * @return a self instance
     * @see .OFFSET
     * @since 3.5.2
     */
    fun LIMIT(value: Long): T {
        return LIMIT(value.toString())
    }

    /**
     * Set the offset variable string(e.g. `"#{offset}"`).
     *
     * @param variable a offset variable string
     * @return a self instance
     * @see .LIMIT
     * @since 3.5.2
     */
    fun OFFSET(variable: String?): T {
        sql().offset = variable
        sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.OFFSET_LIMIT
        return getSelf()
    }

    /**
     * Set the offset value.
     *
     * @param value an offset value
     * @return a self instance
     * @since 3.5.2
     */
    fun OFFSET(value: Long): T {
        return OFFSET(value.toString())
    }

    /**
     * Set the fetch first rows variable string(e.g. `"#{fetchFirstRows}"`).
     *
     * @param variable a fetch first rows variable string
     * @return a self instance
     * @see .OFFSET_ROWS
     * @since 3.5.2
     */
    fun FETCH_FIRST_ROWS_ONLY(variable: String?): T {
        sql().limit = variable
        sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.ISO
        return getSelf()
    }

    /**
     * Set the fetch first rows value.
     *
     * @param value a fetch first rows value
     * @return a self instance
     * @see .OFFSET_ROWS
     * @since 3.5.2
     */
    fun FETCH_FIRST_ROWS_ONLY(value: Int): T {
        return FETCH_FIRST_ROWS_ONLY(value.toString())
    }

    /**
     * Set the offset rows variable string(e.g. `"#{offset}"`).
     *
     * @param variable a offset rows variable string
     * @return a self instance
     * @see .FETCH_FIRST_ROWS_ONLY
     * @since 3.5.2
     */
    fun OFFSET_ROWS(variable: String?): T {
        sql().offset = variable
        sql().limitingRowsStrategy = SQLStatement.LimitingRowsStrategy.ISO
        return getSelf()
    }

    /**
     * Set the offset rows value.
     *
     * @param value an offset rows value
     * @return a self instance
     * @see .FETCH_FIRST_ROWS_ONLY
     * @since 3.5.2
     */
    fun OFFSET_ROWS(value: Long): T {
        return OFFSET_ROWS(value.toString())
    }

    /*
     * used to add a new inserted row while do multi-row insert.
     *
     * @since 3.5.2
     */
    fun ADD_ROW(): T {
        sql().valuesList.add(ArrayList())
        return getSelf()
    }

    fun END(): T {
        sql().end = true
        return getSelf()
    }

    fun ALTER_TABLE(table: String?): T {
        sql().statementType = StatementType.ALTER
        sql().tables.add(table)
        return getSelf()
    }

    fun AUTO_INCREMENT(value: String?): T {
        sql().autoIncrement = value
        return getSelf()
    }

    fun SHOW_CREATE_TABLE(table: String?): T {
        sql().statementType = StatementType.SHOW
        sql().tables.add(table)
        return getSelf()
    }

    private fun sql(): SQLStatement {
        return sql
    }

    fun <A : Appendable> usingAppender(a: A): A {
        sql().sql(a)
        return a
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sql().sql(sb)
        return sb.toString()
    }

    private class SafeAppendable
    (private val a: Appendable) {
        var isEmpty: Boolean = true
            private set

        fun append(s: CharSequence?): SafeAppendable {
            try {
                if (isEmpty && s !!.length > 0) {
                    isEmpty = false
                }
                a.append(s)
            }
            catch (e: IOException) {
                throw RuntimeException(e)
            }
            return this
        }
    }

    private class SQLStatement {
        enum class LimitingRowsStrategy {
            NOP {
                override fun appendClause(builder: SafeAppendable?, offset: String?, limit: String?) {
                    // NOP
                }
            },
            ISO {
                override fun appendClause(builder: SafeAppendable?, offset: String?, limit: String?) {
                    if (offset != null) {
                        builder?.append(" OFFSET ")?.append(offset)?.append(" ROWS")
                    }
                    if (limit != null) {
                        builder?.append(" FETCH FIRST ")?.append(limit)?.append(" ROWS ONLY")
                    }
                }
            },
            OFFSET_LIMIT {
                override fun appendClause(builder: SafeAppendable?, offset: String?, limit: String?) {
                    if (limit != null) {
                        builder?.append("\nLIMIT ")?.append(limit)
                    }
                    if (offset != null) {
                        builder?.append("\nOFFSET ")?.append(offset)
                    }
                }
            };

            abstract fun appendClause(builder: SafeAppendable?, offset: String?, limit: String?)
        }

        var statementType: StatementType? = null
        var sets: MutableList<String?> = ArrayList()
        var select: MutableList<String?> = ArrayList()
        var tables: MutableList<String?> = ArrayList()
        var join: MutableList<String?> = ArrayList()
        var innerJoin: MutableList<String?> = ArrayList()
        var outerJoin: MutableList<String?> = ArrayList()
        var leftOuterJoin: MutableList<String?> = ArrayList()
        var rightOuterJoin: MutableList<String?> = ArrayList()
        var where: MutableList<String?> = ArrayList()
        var having: MutableList<String?> = ArrayList()
        var groupBy: MutableList<String?> = ArrayList()
        var orderBy: MutableList<String?> = ArrayList()
        var lastList: MutableList<String?> = ArrayList()
        var columns: MutableList<String?> = ArrayList()
        var valuesList: MutableList<MutableList<String?>> = ArrayList()
        var distinct: Boolean = false
        var end: Boolean = false
        var autoIncrement: String? = null
        var offset: String? = null
        var limit: String? = null
        var limitingRowsStrategy: LimitingRowsStrategy = LimitingRowsStrategy.NOP
        var engine: String? = null
        var orderByKey: MutableList<String?> = ArrayList()
        val partitionByKey: MutableList<String?> = ArrayList()
        val primaryKey: MutableList<String?> = ArrayList()
        val samplingKey: MutableList<String?> = ArrayList()
        var formatEngine: EngineType? = EngineType.MYSQL
        var lifecycle: String? = null

        init {
            // Prevent Synthetic Access
            valuesList.add(ArrayList())
        }

        private fun sqlClause(builder: SafeAppendable, keyword: String, parts: List<String?>, open: String, close: String, conjunction: String) {
            if (parts.isNotEmpty()) {
                if (! builder.isEmpty) {
                    builder.append("\n")
                }
                builder.append(keyword)
                if (open.isNotEmpty()) {
                    builder.append("")
                }
                else {
                    builder.append(" ")
                }
                builder.append(open)
                var last = "________"
                var i = 0
                val n = parts.size
                while (i < n) {
                    val part = parts[i]
                    if (part != null) {
                        if (i > 0 && part != AND && part != OR && last != AND && last != OR) {
                            builder.append(conjunction)
                        }
                        if (i > 0 && formatEngine?.equals(EngineType.CLICKHOUSE) == true) {
                            builder.append(keyword)
                        }
                        builder.append(part)
                        last = part
                    }
                    else {
                        if (i > 0) {
                            builder.append(conjunction)
                        }
                        builder.append(null)
                    }
                    i ++
                }
                builder.append(close)
            }
        }

        private fun selectSQL(builder: SafeAppendable): String {
            if (distinct) {
                sqlClause(builder, "SELECT DISTINCT", select, "", "", ", ")
            }
            else {
                sqlClause(builder, "SELECT", select, "", "", ", ")
            }

            sqlClause(builder, "FROM", tables, "", "", ", ")
            joins(builder)
            sqlClause(builder, "WHERE", where, "(", ")", " AND ")
            sqlClause(builder, "GROUP BY", groupBy, "", "", ", ")
            sqlClause(builder, "HAVING", having, "(", ")", " AND ")
            sqlClause(builder, "ORDER BY", orderBy, "", "", ", ")
            limitingRowsStrategy.appendClause(builder, offset, limit)
            return builder.toString()
        }

        private fun joins(builder: SafeAppendable) {
            sqlClause(builder, "JOIN", join, "", "", "\nJOIN ")
            sqlClause(builder, "INNER JOIN", innerJoin, "", "", "\nINNER JOIN ")
            sqlClause(builder, "OUTER JOIN", outerJoin, "", "", "\nOUTER JOIN ")
            sqlClause(builder, "LEFT OUTER JOIN", leftOuterJoin, "", "", "\nLEFT OUTER JOIN ")
            sqlClause(builder, "RIGHT OUTER JOIN", rightOuterJoin, "", "", "\nRIGHT OUTER JOIN ")
        }

        private fun insertSQL(builder: SafeAppendable): String {
            sqlClause(builder, "INSERT INTO", tables, "", "", "")
            sqlClause(builder, "", columns, "(", ")", ", ")
            for (i in valuesList.indices) {
                sqlClause(builder, if (i > 0) "," else "VALUES", valuesList[i], "(", ")", ", ")
            }
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun deleteSQL(builder: SafeAppendable): String {
            sqlClause(builder, "DELETE FROM", tables, "", "", "")
            sqlClause(builder, "WHERE", where, "(", ")", " AND ")
            limitingRowsStrategy.appendClause(builder, null, limit)
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun updateSQL(builder: SafeAppendable): String {
            sqlClause(builder, "UPDATE", tables, "", "", "")
            joins(builder)
            sqlClause(builder, "SET", sets, "", "", ", ")
            sqlClause(builder, "WHERE", where, "(", ")", " AND ")
            limitingRowsStrategy.appendClause(builder, null, limit)
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun alterSQL(builder: SafeAppendable): String {
            sqlClause(builder, "ALTER TABLE", tables, "", "", "")
            sqlClause(builder, "AUTO_INCREMENT", listOf(autoIncrement), "= ", "", "")
            limitingRowsStrategy.appendClause(builder, null, limit)
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun showSQL(builder: SafeAppendable): String {
            sqlClause(builder, "SHOW CREATE TABLE", tables, "", "", "")
            limitingRowsStrategy.appendClause(builder, null, limit)
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun truncateSQL(builder: SafeAppendable): String {
            sqlClause(builder, "TRUNCATE TABLE", tables, "", "", "")
            limitingRowsStrategy.appendClause(builder, null, limit)
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun dropSQL(builder: SafeAppendable): String {
            sqlClause(builder, "DROP TABLE", tables, "", "", "")
            limitingRowsStrategy.appendClause(builder, null, limit)
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun createTableSQL(builder: SafeAppendable): String {
            sqlClause(builder, "CREATE TABLE", tables, "", "", "")
            sqlClause(builder, "", columns, "(\n", "\n)", ",\n")
            if (engine != null) {
                sqlClause(builder, "ENGINE", listOf(engine), " = ", "", ",\n")
            }
            if (orderByKey.isNotEmpty()) {
                sqlClause(builder, "ORDER BY", orderByKey, "(", ")", ", ")
            }
            if (partitionByKey.isNotEmpty()) {
                sqlClause(builder, "PARTITION BY", partitionByKey, "(", ")", ", ")
            }
            if (primaryKey.isNotEmpty()) {
                sqlClause(builder, "PRIMARY KEY", primaryKey, "(", ")", ", ")
            }
            if (samplingKey.isNotEmpty()) {
                sqlClause(builder, "SAMPLE BY", samplingKey, "(", ")", ", ")
            }
            if (lifecycle != null) {
                sqlClause(builder, "TTL", listOf(lifecycle), "", "", ", ")
            }
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun createColumnSQL(builder: SafeAppendable): String {
            sqlClause(builder, "ALTER TABLE", tables, "", "", "")
            sqlClause(builder, "ADD COLUMN", columns, "(", ")", ",\n")
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun dropColumnSQL(builder: SafeAppendable): String {
            sqlClause(builder, "ALTER TABLE", tables, "", "", "")
            sqlClause(builder, "DROP COLUMN", columns, "", "", ",\n")
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun modifyColumnSQL(builder: SafeAppendable): String {
            sqlClause(builder, "ALTER TABLE", tables, "", "", "")
            if (formatEngine == EngineType.CLICKHOUSE) {
                sqlClause(builder, "MODIFY COLUMN", columns, "", "", ",\n")
            }
            else {
                sqlClause(builder, "MODIFY", columns, "", "", ",\n")
            }
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        private fun modifyLifecycleSQL(builder: SafeAppendable): String {
            sqlClause(builder, "ALTER TABLE", tables, "", "", "")
            sqlClause(builder, "MODIFY TTL", listOf(lifecycle), "", "", ",\n")
            if (end) {
                builder.append(";")
            }
            return builder.toString()
        }

        fun sql(a: Appendable): String? {
            val builder = SafeAppendable(a)

            val answer = when (statementType) {
                StatementType.DELETE -> deleteSQL(builder)
                StatementType.INSERT -> insertSQL(builder)
                StatementType.SELECT -> selectSQL(builder)
                StatementType.UPDATE -> updateSQL(builder)
                StatementType.ALTER -> alterSQL(builder)
                StatementType.SHOW -> showSQL(builder)
                StatementType.TRUNCATE -> truncateSQL(builder)
                StatementType.DROP -> dropSQL(builder)
                StatementType.CREATE_TABLE -> createTableSQL(builder)
                StatementType.CREATE_COLUMN -> createColumnSQL(builder)
                StatementType.DROP_COLUMN -> dropColumnSQL(builder)
                StatementType.MODIFY_COLUMN -> modifyColumnSQL(builder)
                StatementType.MODIFY_LIFECYCLE -> modifyLifecycleSQL(builder)
                else -> throw SqlException("Unsupported statement type: [ $statementType ]")
            }
            return answer
        }
    }
}
