package org.devlive.metaforge.parser.model

class Column
{
    private var _functions = mutableSetOf<String>()

    var name: String? = null
    var type: String? = null
    var alias: String? = null
    var expression: String? = null

    var functions: Set<String>
        get() = _functions.toSet()
        set(value)
        {
            _functions = value.toMutableSet()
        }

    fun copy(): Column
    {
        val newColumn = Column()
        newColumn.name = this.name
        newColumn.type = this.type
        newColumn.alias = this.alias
        newColumn.expression = this.expression
        newColumn.functions = this.functions
        return newColumn
    }

    override fun toString(): String = "Column(name=$name, type=$type, alias=$alias, expression=$expression, functions=$functions)"
}
