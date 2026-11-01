package org.devlive.metaforge.convert.model

import java.io.InputStream

class ConvertRequest
{
    private var _headers: List<Any> = listOf()
    private var _columns: List<Any> = listOf()
    private var _stream: InputStream? = null
    private var _types: List<Any> = listOf()

    var name: String? = null
    var path: String? = null

    // Currently only valid for txt format
    var delimiter: String? = null

    // The format type of the original data
    var format: String? = null

    // Column types in header order, written as field definitions when includeStructure is set (writer only)
    var types: List<Any>
        get() = _types.toList()
        set(value)
        {
            _types = value.toMutableList()
        }

    // Write the column names and types ahead of the rows, in the way the format allows (writer only)
    var includeStructure: Boolean = false

    // Leave NULL values out: omitted fields in JSON and XML, empty cells in CSV and TXT (writer only)
    var excludeNull: Boolean = false

    var headers: List<Any>
        get() = _headers.toList()
        set(value)
        {
            _headers = value.toMutableList()
        }

    var columns: List<Any>
        get() = _columns.toList()
        set(value)
        {
            _columns = value.toMutableList()
        }

    var stream: InputStream?
        get() = _stream
        set(value)
        {
            _stream = value
        }
}
