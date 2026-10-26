package org.devlive.metaforge.plugin.dolphindb

import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.temporal.ChronoUnit

class DolphinDBContainer(dockerImageName: String = IMAGE) : GenericContainer<DolphinDBContainer>(DockerImageName.parse(dockerImageName))
{
    companion object
    {
        private val LOG = LoggerFactory.getLogger(DolphinDBContainer::class.java)
        private const val IMAGE = "dolphindb/dolphindb-arm64:v2.00.7"
        private const val DEFAULT_PORT = 8848
    }

    init
    {
        // 配置端口
        // Configure ports
        withExposedPorts(DEFAULT_PORT)

        // 设置启动超时
        // Set startup timeout
        withStartupTimeout(Duration.of(120, ChronoUnit.SECONDS))

        // 等待策略 - 等待端口可用
        // Waiting strategy - wait for port
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))

        // 添加平台兼容性支持
        // Add platform support
        withCreateContainerCmdModifier { cmd ->
            cmd.withPlatform("linux/amd64")
        }

        // 设置主机名，用于企业版许可证的机器指纹收集
        // Set hostname for Enterprise Edition License machine fingerprint collection
        withCreateContainerCmdModifier { cmd ->
            cmd.withHostName("cnserver10")
        }

        // 挂载 /etc 目录用于许可证机器指纹收集
        // Mount /etc directory for license machine fingerprint collection
        withFileSystemBind("/etc", "/dolphindb/etc")

        // 设置工作目录
        // Set working directory
        withWorkingDirectory("/data/ddb")

        // 使用官方镜像的启动命令
        // Use official image startup command
        withCommand("sh")
    }

    override fun start()
    {
        super.start()
        LOG.info("DolphinDB container started successfully")
        LOG.info("DolphinDB Web UI available at: http://{}:{}", host, getMappedPort(DEFAULT_PORT))
    }

    override fun stop()
    {
        LOG.info("Stopping DolphinDB container")
        super.stop()
    }

    val port: Int
        get() = getMappedPort(DEFAULT_PORT)
}
