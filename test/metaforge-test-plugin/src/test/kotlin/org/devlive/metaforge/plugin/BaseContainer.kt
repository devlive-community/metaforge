package org.devlive.metaforge.plugin

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.slf4j.LoggerFactory
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@SuppressFBWarnings(value = ["CT_CONSTRUCTOR_THROW"])
open class BaseContainer<SELF : BaseContainer<SELF>> : GenericContainer<SELF>
{
    private val containerPorts: MutableMap<String, Int> = mutableMapOf()

    companion object
    {
        private val logger = LoggerFactory.getLogger(BaseContainer::class.java)
    }

    @Throws(ContainerLaunchException::class)
    protected constructor(imageName: String, ports: Map<String, Int>) : super(DockerImageName.parse(imageName))
    {
        try
        {
            containerPorts.putAll(ports)
            setupContainer()
        }
        catch (e: Exception)
        {
            logger.error("Failed to initialize container", e)
            throw ContainerLaunchException("Failed to initialize container", e)
        }
    }

    @Throws(ContainerLaunchException::class)
    protected constructor(dockerImageName: DockerImageName, ports: Map<String, Int>) : super(dockerImageName)
    {
        try
        {
            dockerImageName.assertCompatibleWith(dockerImageName)
            containerPorts.putAll(ports)
            setupContainer()
        }
        catch (e: Exception)
        {
            logger.error("Failed to initialize container", e)
            throw ContainerLaunchException("Failed to initialize container", e)
        }
    }

    private fun setupContainer()
    {
        withExposedPorts(*containerPorts.values.toTypedArray())
    }

    fun getPort(name: String): Int
    {
        return getMappedPort(
            containerPorts[name] ?: throw IllegalArgumentException("Port $name not found")
        )
    }

    fun getContainerPort(name: String): Int
    {
        return containerPorts[name] ?: throw IllegalArgumentException("Port $name not found")
    }

    fun getAllPorts(): Map<String, Int>
    {
        return containerPorts.mapValues { (_, port) -> getMappedPort(port) }
    }

    fun getAllContainerPorts(): Map<String, Int>
    {
        return containerPorts.toMap()
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (other !is BaseContainer<*>) return false
        if (! super.equals(other)) return false

        return containerPorts == other.containerPorts
    }

    override fun hashCode(): Int
    {
        var result = super.hashCode()
        result = 31 * result + containerPorts.hashCode()
        return result
    }
}
