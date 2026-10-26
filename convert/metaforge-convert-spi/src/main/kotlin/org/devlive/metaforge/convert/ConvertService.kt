package org.devlive.metaforge.convert

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

    fun format(request: ConvertRequest): ConvertResponse

    fun formatStream(request: ConvertRequest): ConvertResponse

    fun writer(request: ConvertRequest): ConvertResponse

    fun reader(request: ConvertRequest): ConvertResponse
}
