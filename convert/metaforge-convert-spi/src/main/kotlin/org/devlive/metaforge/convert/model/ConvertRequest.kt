package org.devlive.metaforge.convert.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.InputStream

@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "A request carries the caller's values")
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

    // Values of the plugin's declared options (see ConvertService.options), by option name
    var options: Map<String, String> = mapOf()

    /** An option's value, or [fallback] when it was not given. */
    fun option(name: String, fallback: String): String
    {
        return options[name]?.takeIf { it.isNotEmpty() } ?: fallback
    }

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
