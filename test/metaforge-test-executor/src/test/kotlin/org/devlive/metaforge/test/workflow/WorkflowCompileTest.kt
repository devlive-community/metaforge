package org.devlive.metaforge.test.workflow

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.metaforge.MetaForgeExecutorService
import org.devlive.metaforge.executor.seatunnel.SeatunnelExecutorService
import org.devlive.metaforge.executor.workflow.WorkflowCategory
import org.devlive.metaforge.executor.workflow.WorkflowNode
import org.devlive.metaforge.executor.workflow.WorkflowPlan
import org.devlive.metaforge.test.metaforge.support.FakePluginService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each executor ships its workflow declaration; MetaForge compiles plans into its own requests. */
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class WorkflowCompileTest
{
    private fun node(type: String, key: String, category: WorkflowCategory, values: Map<String, Any?>, sources: Map<String, ExecutorConfigure> = emptyMap()) =
        WorkflowNode(type + "-id", type, key, category, type, values, sources)

    private fun saved(streaming: Boolean = true) = ExecutorConfigure("Fake").apply { plugin = FakePluginService(streaming = streaming) }

    @Test
    fun executorsShipTheirOwnDeclarations()
    {
        val metaforge = MetaForgeExecutorService().workflowDeclaration()
        val seatunnel = SeatunnelExecutorService().workflowDeclaration()
        assertNotNull(metaforge)
        assertNotNull(seatunnel)
        assertTrue(metaforge.toString().contains("metaforge-executor-metaforge"))
        assertTrue(seatunnel.toString().contains("metaforge-executor-seatunnel"))
        assertNotNull(java.net.URL(metaforge, "nodes/metaforge-source.yaml").openStream().use { it.read() })
    }

    @Test
    fun metaforgePlanCarriesTheTargetMappingAndWorkflowSettings()
    {
        val input = saved()
        val output = saved()
        val plan = WorkflowPlan(
            "run", "owner", "/tmp/work", 120,
            listOf(
                node("metaforge-source", "Source", WorkflowCategory.SOURCE, mapOf("source" to "A", "query" to "select id from events"), mapOf("source" to input)),
                node("metaforge-sink", "Sink", WorkflowCategory.SINK, mapOf("source" to "B", "database" to "dw", "table" to "events", "columns" to linkedMapOf("id" to "event_id", "name" to "event_name")), mapOf("source" to output))
            ),
            mapOf("batchSize" to 200, "parallelism" to 3, "preCount" to null),
            mapOf("batchSize" to "1000", "fetchSize" to "500")
        )
        val request = MetaForgeExecutorService().compile(plan)
        assertEquals("select id from events", request.input.query)
        assertEquals("dw", request.input.database)
        assertEquals("events", request.input.table)
        assertEquals(listOf("event_id", "event_name"), request.input.originColumns.map { it.name })
        assertEquals(listOf("id", "name"), request.input.originColumns.map { it.original })
        assertEquals("200", request.options["batchSize"])
        assertEquals("500", request.options["fetchSize"])
        assertFalse(request.options.containsKey("parallelism"))
        // Workflows count the source first unless the workflow turns it off.
        assertEquals("true", request.options["preCount"])
        assertEquals("false", MetaForgeExecutorService().compile(plan.copy(settings = mapOf("preCount" to false))).options["preCount"])
        assertEquals("50", request.options["previewRows"])
        assertEquals(120, request.timeout)
        assertEquals("run", request.taskName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun metaforgeRejectsTargetsWithoutBatchWriting()
    {
        MetaForgeExecutorService().compile(WorkflowPlan("run", "owner", null, 1, listOf(
            node("metaforge-source", "Source", WorkflowCategory.SOURCE, mapOf("query" to "select 1"), mapOf("source" to saved())),
            node("metaforge-sink", "Sink", WorkflowCategory.SINK, mapOf("database" to "d", "table" to "t", "columns" to mapOf("a" to "b")), mapOf("source" to saved(false)))
        )))
    }
}
