package org.devlive.metaforge.test.spi

import org.devlive.metaforge.executor.ExecutorService
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.plugin.annotation.InjectService

@InjectService
class NoneExecutorService : ExecutorService
{
    override fun start(request: ExecutorRequest): ExecutorResponse
    {
        return ExecutorResponse()
    }

    override fun stop(request: ExecutorRequest): ExecutorResponse
    {
        return ExecutorResponse()
    }
}
