package org.devlive.metaforge.notify

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.notify.model.NotifyRequest
import org.junit.Assert
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class NotifyModuleTest
{
    private val log: Logger = LoggerFactory.getLogger(TestNotify::class.java)
    private val injector: Injector = Guice.createInjector(NotifyManager())

    @Test
    fun test()
    {
        val optional: Optional<Notify> = injector.getInstance(Key.get(object : TypeLiteral<Set<Notify>>()
        {}))
            .stream()
            .findFirst()

        optional.ifPresent {
            val request = NotifyRequest()
            request.title = "This is title"
            request.content = "This is content"
            request.sendTo = "community@devlive.org"
            log.info("Notify response: {}", it.send(request))
            Assert.assertEquals("Test", it.name())
        }
    }
}
