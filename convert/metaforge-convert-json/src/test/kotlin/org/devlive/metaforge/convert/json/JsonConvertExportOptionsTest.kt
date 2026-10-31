package org.devlive.metaforge.convert.json

import org.devlive.metaforge.convert.model.ConvertRequest
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonConvertExportOptionsTest
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
            val response = JsonConvertService().writer(request)
            assertTrue(response.successful == true, response.message ?: "")
            return File(response.path ?: File(directory, "rows.json").absolutePath).readText()
        }
        finally
        {
            directory.deleteRecursively()
        }
    }

    @Test
    fun structureAndNullOptionsShapeTheFile()
    {
        assertEquals("""[{"id":1,"name":null},{"id":2,"name":"b"}]""", write(false, false))
        assertEquals("""{"columns":[{"name":"id","type":"BIGINT"},{"name":"name","type":"VARCHAR"}],"rows":[{"id":1},{"id":2,"name":"b"}]}""", write(true, true))
    }
}
