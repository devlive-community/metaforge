package org.devlive.metaforge.sql.model

import org.devlive.metaforge.sql.SqlException

class Column {
    var name: String? = null
    var type: String? = null
    var length: Int = 0
    var comment: String? = null
    var defaultValue: String? = null
    var primaryKey: Boolean = false
    var autoIncrement: Boolean = false
    var isNullable: Boolean = false

    /**
     * Convert to column variable
     * <p> Example:
     * <p> column_name column_type
     * <p> column_name column_type(column_length)
     */
    fun toColumnVar(): String {
        if (this.type == null || this.type!!.isEmpty()) {
            throw SqlException("Column type cannot be empty")
        }
        var str = "$name $type"
        if (this.length > 0) {
            str = "$str($length)"
        }
        if (this.isNullable) {
            str = "$str NOT NULL"
        }
        if (this.primaryKey) {
            str = "$str PRIMARY KEY"
        }
        if (this.autoIncrement) {
            str = "$str AUTO_INCREMENT"
        }
        if (this.comment != null && this.comment!!.isNotEmpty()) {
            str = "$str COMMENT '$comment'"
        }
        if (this.defaultValue != null && this.defaultValue!!.isNotEmpty()) {
            str = "$str DEFAULT '$defaultValue'"
        }
        return str
    }
}
