package org.devlive.metaforge.convert.csv

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.file.File
import org.devlive.metaforge.file.FileManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvModuleTest
{
    private val injector: Injector = Guice.createInjector(FileManager())

    @Test
    fun test()
    {
        injector.getInstance(Key.get(object : TypeLiteral<Set<File>>()
        {}))
            .stream()
            .findFirst()
            .ifPresent {
                assertEquals("Csv", it.name())
            }
    }
}
