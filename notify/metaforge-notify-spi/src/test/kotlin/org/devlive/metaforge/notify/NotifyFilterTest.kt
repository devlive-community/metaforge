package org.devlive.metaforge.notify

import com.google.inject.Guice
import org.junit.Assert
import org.junit.Test

class NotifyFilterTest
{
    private val injector = Guice.createInjector(NotifyManager())

    @Test
    fun testFindNotify()
    {
        Assert.assertNotNull(NotifyFilter.findNotify(injector, "Test"))
    }
}
