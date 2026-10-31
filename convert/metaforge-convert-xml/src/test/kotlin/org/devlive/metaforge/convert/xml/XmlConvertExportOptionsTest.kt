package org.devlive.metaforge.convert.xml

import org.devlive.metaforge.convert.model.ConvertRequest
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmlConvertExportOptionsTest
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
            val response = XmlConvertService().writer(request)
            assertTrue(response.successful == true, response.message ?: "")
            return File(response.path ?: File(directory, "rows.xml").absolutePath).readText()
        }
        finally
        {
            directory.deleteRecursively()
        }
    }

    @Test
    fun structureAndNullOptionsShapeTheFile()
    {
        val plain = write(false, false)
        assertTrue(plain.contains("""<Node id="1" name="null"/>"""), plain)
        val shaped = write(true, true)
        assertTrue(shaped.contains("""<Columns><Column name="id" type="BIGINT"/><Column name="name" type="VARCHAR"/></Columns>"""), shaped)
        assertTrue(shaped.contains("""<Node id="1"/>"""), shaped)
    }
}
