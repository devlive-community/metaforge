package org.devlive.metaforge.convert

import org.devlive.metaforge.convert.model.ConvertRequest
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TxtConvertExportOptionsTest
{
    /** Writes two rows, the first with a NULL name, and returns the file. */
    private fun write(includeStructure: Boolean, excludeNull: Boolean): String
    {
        val directory = Files.createTempDirectory("convert-options").toFile()
        try
        {
            val request = ConvertRequest()
            request.name = "rows"
            request.path = directory.absolutePath
            request.delimiter = "\t"
            request.headers = listOf("id", "name")
            request.types = listOf("BIGINT", "VARCHAR")
            request.columns = listOf(listOf(1, null), listOf(2, "b"))
            request.includeStructure = includeStructure
            request.excludeNull = excludeNull
            val response = TxtConvertService().writer(request)
            assertTrue(response.successful == true, response.message ?: "")
            return File(response.path ?: File(directory, "rows.txt").absolutePath).readText()
        }
        finally
        {
            directory.deleteRecursively()
        }
    }

    @Test
    fun structureAndNullOptionsShapeTheFile()
    {
        assertEquals("id\tname\n1\tnull\n2\tb", write(false, false))
        assertEquals("id\tname\nBIGINT\tVARCHAR\n1\t\n2\tb", write(true, true))
    }
}
