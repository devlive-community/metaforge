package org.devlive.metaforge.executor.seatunnel

import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Seatunnel 安全网：锁定“ExecutorRequest -> seatunnel 命令行”的映射。
 *
 * ExecutorRequest 已把 executor 专属项收敛进 options（home/startScript/way/mode/engine）。
 * 这里断言最终命令字符串——只要 buildCommander 从 options 读到等价的值，命令就不变、测试保持绿。
 * 命令期望值与字段拆分前完全一致，用来证明拆分没有改变行为。
 */
class SeatunnelCommanderMappingTest
{
    private val service = SeatunnelExecutorService()

    @Test
    fun sparkEngineCommand()
    {
        val request = ExecutorRequest(
            taskName = "task1",
            userName = "tester",
            input = ExecutorConfigure("Jdbc"),
            output = ExecutorConfigure("Jdbc"),
            workHome = "/work",
            options = mapOf(
                "home" to "/opt/seatunnel",
                "startScript" to "start-seatunnel-spark-connector-v2.sh",
                "way" to "LOCAL",
                "mode" to "CLIENT",
                "engine" to "SPARK"
            )
        )

        val config = "/work" + File.separator + "task1.configure"
        assertEquals(
            "/opt/seatunnel/bin/start-seatunnel-spark-connector-v2.sh " +
                    "--master local --deploy-mode client --config $config --name task1",
            service.buildCommander(request).toCommand()
        )
    }

    @Test
    fun seatunnelEngineLocalCommand()
    {
        val request = ExecutorRequest(
            taskName = "task2",
            userName = "tester",
            input = ExecutorConfigure("Jdbc"),
            output = ExecutorConfigure("Jdbc"),
            workHome = "/work",
            options = mapOf(
                "home" to "/opt/seatunnel",
                "startScript" to "seatunnel.sh",
                "way" to "LOCAL",
                "mode" to "CLIENT",
                "engine" to "SEATUNNEL"
            )
        )

        val config = "/work" + File.separator + "task2.configure"
        assertEquals(
            "/opt/seatunnel/bin/seatunnel.sh -e local --config $config --name task2",
            service.buildCommander(request).toCommand()
        )
    }
}
