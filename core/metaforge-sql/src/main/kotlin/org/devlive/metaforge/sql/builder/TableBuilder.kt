package org.devlive.metaforge.sql.builder

import org.devlive.metaforge.sql.BaseSql

class TableBuilder {
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

        fun CREATE_TABLE(table: String?) {
            sql().CREATE_TABLE(table)
        }

        fun MODIFY_LIFECYCLE(table: String?) {
            sql().MODIFY_LIFECYCLE(table)
        }

        fun LIFECYCLE(lifecycle: String?) {
            sql().LIFECYCLE(lifecycle)
        }

        fun ADD_LIFECYCLE(lifecycle: String?) {
            sql().ADD_LIFECYCLE(lifecycle)
        }

        fun COLUMNS(values: List<String>) {
            sql().COLUMNS(values)
        }

        fun ENGINE(engine: String?) {
            sql().ENGINE(engine)
        }

        fun ORDER_BY(values: List<String>) {
            sql().ORDER_BY_KEY(values)
        }

        fun PARTITION_BY(values: List<String>) {
            sql().PARTITION_BY_KEY(values)
        }

        fun PRIMARY_KEY(values: List<String>) {
            sql().PRIMARY_KEY(values)
        }

        fun SAMPLING_KEY(values: List<String>) {
            sql().SAMPING_KEY(values)
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
