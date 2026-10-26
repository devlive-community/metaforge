package org.devlive.metaforge.convert.model

class ConvertResponse
{
    private var _headers: List<Any> = listOf()
    private var _columns: List<Any> = listOf()

    var successful: Boolean? = true
    var message: String? = null
    var path: String? = null

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

    override fun toString(): String
    {
        return "ConvertResponse(path=$path, message=$message, successful=$successful)"
    }
}
