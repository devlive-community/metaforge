package org.devlive.metaforge.sql.builder

import org.devlive.metaforge.sql.BaseSql
import org.devlive.metaforge.sql.EngineType

class ColumnBuilder {
    init {
        BEGIN();
    }

    companion object {
        private val localSQL: ThreadLocal<BaseSql> = ThreadLocal<BaseSql>()

        private fun sql(): BaseSql {
            return localSQL.get()
        }

        fun BEGIN() {
            RESET()
        }

        fun RESET() {
            localSQL.set(BaseSql())
        }

        fun CREATE_COLUMN(table: String?) {
            sql().CREATE_COLUMN(table)
        }

        fun DROP_COLUMN(table: String?) {
            sql().DROP_COLUMN(table)
        }

        fun MODIFY_COLUMN(table: String?) {
            sql().MODIFY_COLUMN(table)
        }

        fun COLUMNS(columns: List<String>) {
            sql().COLUMNS(columns)
        }

        fun FORMAT_ENGINE(engine: EngineType?) {
            sql().FORMAT_ENGINE(engine)
        }

        fun END() {
            sql().END()
        }

        fun SQL(): String {
            try {
                return sql().toString()
            }
            finally {
                RESET()
            }
        }
    }
}
