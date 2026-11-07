package org.devlive.metaforge.executor.seatunnel

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import com.fasterxml.jackson.databind.ObjectMapper
import org.devlive.metaforge.executor.workflow.WorkflowCategory
import org.devlive.metaforge.executor.workflow.WorkflowNode
import org.devlive.metaforge.executor.workflow.WorkflowPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/** SeaTunnel compiles a pipeline into one job whose blocks are joined by result tables. */
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class SeatunnelWorkflowCompileTest
{
    private fun node(type: String, key: String, category: WorkflowCategory, values: Map<String, Any?>) =
        WorkflowNode(type + "-id", type, key, category, type, values)

    @Test
    fun seatunnelChainsTransformsThroughResultTables()
    {
        val plan = WorkflowPlan(
            "run", "owner", null, 60,
            listOf(
                node("jdbc-source", "Jdbc", WorkflowCategory.SOURCE, mapOf("url" to "jdbc:x", "fetch_size" to 100, "password" to "")),
                node("sql-transform", "Sql", WorkflowCategory.TRANSFORM, mapOf("query" to "select * from \${input} where id > 1")),
                node("replace-transform", "Replace", WorkflowCategory.TRANSFORM, mapOf("replace_field" to "name", "source_table_name" to "named")),
                node("jdbc-sink", "Jdbc", WorkflowCategory.SINK, mapOf("url" to "jdbc:y"))
            ),
            mapOf("parallelism" to 4.0)
        )
        val service = SeatunnelExecutorService()
        assertTrue(service.validate(plan).isEmpty())
        val request = service.compile(plan)
        val transforms = request.transformChain()
        assertEquals(listOf("Sql", "Replace"), transforms.map { it.type })
        assertEquals("workflow_input", request.input.configure!!["result_table_name"])
        assertNull(request.input.configure!!["password"])
        assertEquals(100, request.input.configure!!["fetch_size"])
        assertEquals("workflow_input", transforms[0].configure!!["source_table_name"])
        assertEquals("select * from workflow_input where id > 1", transforms[0].configure!!["query"])
        assertEquals("named", transforms[0].configure!!["result_table_name"])
        assertEquals("named", transforms[1].configure!!["source_table_name"])
        assertEquals("workflow_output", transforms[1].configure!!["result_table_name"])
        assertEquals("workflow_output", request.output.configure!!["source_table_name"])
        assertEquals("4", request.options["env.parallelism"])

        val directory = Files.createTempDirectory("metaforge-workflow-chain")
        try
        {
            request.workHome = directory.toString()
            assertNull(service.before(request, org.slf4j.LoggerFactory.getLogger(javaClass)))
            val root = ObjectMapper().readTree(directory.resolve("run.configure").toFile())
            assertEquals(4, root["env"]["parallelism"].asInt())
            assertTrue(root["env"]["parallelism"].isNumber)
            assertEquals(listOf("Sql", "Replace"), root["transform"].fieldNames().asSequence().toList())
            assertEquals("named", root["transform"]["Replace"]["source_table_name"].asText())
        }
        finally
        {
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        }
    }

    @Test
    fun seatunnelRejectsRepeatedTransformsAndMismatchedTables()
    {
        val repeated = WorkflowPlan("run", "owner", null, 60, listOf(
            node("jdbc-source", "Jdbc", WorkflowCategory.SOURCE, mapOf("result_table_name" to "a")),
            node("sql-transform", "Sql", WorkflowCategory.TRANSFORM, mapOf("source_table_name" to "b")),
            node("sql-transform", "Sql", WorkflowCategory.TRANSFORM, emptyMap()),
            node("jdbc-sink", "Jdbc", WorkflowCategory.SINK, emptyMap())
        ))
        val errors = SeatunnelExecutorService().validate(repeated)
        assertEquals(2, errors.size)
    }

    @Test
    fun seatunnelWithoutTransformsKeepsUserTableNames()
    {
        val request = SeatunnelExecutorService().compile(WorkflowPlan("run", "owner", null, 60, listOf(
            node("jdbc-source", "Jdbc", WorkflowCategory.SOURCE, mapOf("url" to "jdbc:x")),
            node("jdbc-sink", "Jdbc", WorkflowCategory.SINK, mapOf("url" to "jdbc:y"))
        )))
        assertTrue(request.transformChain().isEmpty())
        assertNull(request.input.configure!!["result_table_name"])
        assertNull(request.output.configure!!["source_table_name"])
        assertFalse(request.options.keys.any { it.startsWith("env.") })
    }
}
