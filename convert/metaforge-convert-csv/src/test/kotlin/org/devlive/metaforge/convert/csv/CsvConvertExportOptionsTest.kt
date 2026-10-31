package org.devlive.metaforge.convert.csv

import org.devlive.metaforge.convert.model.ConvertRequest
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvConvertExportOptionsTest
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
            val response = CsvConvertService().writer(request)
            assertTrue(response.successful == true, response.message ?: "")
            return File(response.path ?: File(directory, "rows.csv").absolutePath).readText()
        }
        finally
        {
            directory.deleteRecursively()
        }
    }

    @Test
    fun structureAndNullOptionsShapeTheFile()
    {
        assertEquals("id,name\n1,null\n2,b", write(false, false))
        assertEquals("id,name\nBIGINT,VARCHAR\n1,\n2,b", write(true, true))
    }
}
