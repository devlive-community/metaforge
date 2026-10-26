package org.devlive.metaforge.test.spi

import org.devlive.metaforge.convert.ConvertService
import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.model.ConvertResponse
import lombok.extern.slf4j.Slf4j

@Slf4j
class NoneConvertService : ConvertService
{
    override fun format(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        response.successful = true
        return response
    }

    override fun formatStream(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        response.successful = true
        return response
    }

    override fun writer(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        response.successful = true
        return response
    }

    override fun reader(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        response.successful = true
        return response
    }
}
