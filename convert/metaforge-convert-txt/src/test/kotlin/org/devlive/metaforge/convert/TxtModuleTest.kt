package org.devlive.metaforge.convert

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.convert.Convert
import org.devlive.metaforge.convert.ConvertManager
import org.junit.Assert
import org.junit.Test

class TxtModuleTest
{
    private val injector: Injector = Guice.createInjector(ConvertManager())

    @Test
    fun test()
    {
        injector.getInstance(Key.get(object : TypeLiteral<Set<Convert>>()
        {}))
            .stream()
            .findFirst()
            .ifPresent {
                Assert.assertEquals("Txt", it.name())
            }
    }
}
