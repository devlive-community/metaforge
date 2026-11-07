package org.devlive.metaforge.executor.seatunnel

import com.fasterxml.jackson.databind.ObjectMapper
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.Properties

class SeatunnelWorkflowConfigurationTest
{
    @Test
    fun workflowConfigurationPreservesTypedFieldsAndMultilineSql()
    {
        val directory = Files.createTempDirectory("metaforge-workflow-config")
        try
        {
            val input = ExecutorConfigure("Jdbc", Properties().apply {
                put("query", "SELECT id, name\nFROM events")
                put("fetch_size", 1000)
                put("properties", mapOf("ssl" to true))
            })
            val output = ExecutorConfigure("Jdbc", Properties().apply {
                put("primary_keys", listOf("id", "name"))
                put("auto_commit", false)
                put("batch_size", 500)
            })
            val request = ExecutorRequest("workflow-run", "owner", input, output)
            request.workHome = directory.toString()
            assertNull(SeatunnelExecutorService().before(request, LoggerFactory.getLogger(javaClass)))
            val root = ObjectMapper().readTree(directory.resolve("workflow-run.configure").toFile())
            assertEquals("SELECT id, name\nFROM events", root["source"]["Jdbc"]["query"].asText())
            assertTrue(root["source"]["Jdbc"]["fetch_size"].isNumber)
            assertTrue(root["source"]["Jdbc"]["properties"]["ssl"].asBoolean())
            assertTrue(root["sink"]["Jdbc"]["primary_keys"].isArray)
            assertEquals(2, root["sink"]["Jdbc"]["primary_keys"].size())
            assertEquals(false, root["sink"]["Jdbc"]["auto_commit"].asBoolean())
        }
        finally
        {
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        }
    }
}
