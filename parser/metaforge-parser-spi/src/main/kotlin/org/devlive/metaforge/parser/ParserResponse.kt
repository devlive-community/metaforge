package org.devlive.metaforge.parser

import org.devlive.metaforge.parser.model.Table
import org.devlive.metaforge.parser.type.EngineType
import org.devlive.metaforge.parser.type.StatementType

class ParserResponse
{
    private var _table: Table = Table()
    var type: StatementType = StatementType.UNKNOWN
    var engine: EngineType = EngineType.MYSQL
    var isParser: Boolean = false
    var message: String? = null

    var table: Table
        get() = _table.copy()
        set(value)
        {
            _table = value.copy()
        }

    override fun toString(): String = "ParserResponse(table=$table, type=$type, engine=$engine)"
}
