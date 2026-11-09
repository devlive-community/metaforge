package org.devlive.metaforge.convert

import org.devlive.metaforge.convert.model.ConvertOption
import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.model.ConvertResponse
import org.devlive.metaforge.plugin.Service

interface ConvertService : Service
{
    fun name(): String
    {
        return this.javaClass
            .simpleName
            .removeSuffix("ConvertService")
    }

    fun description(): String
    {
        return "Integrate ${name()} format convert"
    }

    /** Writer settings the plugin offers; values arrive in [ConvertRequest.options]. */
    fun options(): List<ConvertOption>
    {
        return listOf()
    }

    /** Extension of the files the writer produces. */
    fun extension(): String
    {
        return name().lowercase()
    }

    fun format(request: ConvertRequest): ConvertResponse

    fun formatStream(request: ConvertRequest): ConvertResponse

    fun writer(request: ConvertRequest): ConvertResponse

    fun reader(request: ConvertRequest): ConvertResponse
}
