package org.devlive.metaforge.parser.model

class Table
{
    private var _columns: Collection<Column> = mutableSetOf()

    var database: String? = null
    var name: String? = null
    var alias: String? = null
    var limit: Long? = null

    var columns: Set<Column>
        get() = _columns.toSet()
        set(value)
        {
            _columns = value.toMutableSet()
        }

    fun copy(): Table
    {
        val newTable = Table()
        newTable.database = this.database
        newTable.name = this.name
        newTable.alias = this.alias
        newTable._columns = this._columns.mapTo(HashSet()) { it.copy() }
        newTable.limit = this.limit
        return newTable
    }

    override fun toString(): String = "Table(database=$database, name=$name, alias=$alias, columns=$columns, limit=$limit)"
}
