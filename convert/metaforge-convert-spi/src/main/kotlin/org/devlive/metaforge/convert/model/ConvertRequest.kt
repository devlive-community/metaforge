package org.devlive.metaforge.convert.model

import java.io.InputStream

class ConvertRequest
{
    private var _headers: List<Any> = listOf()
    private var _columns: List<Any> = listOf()
    private var _stream: InputStream? = null

    var name: String? = null
    var path: String? = null

    // Currently only valid for txt format
    var delimiter: String? = null

    // The format type of the original data
    var format: String? = null

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
