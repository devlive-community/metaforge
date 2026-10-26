package org.devlive.metaforge.plugin.influxdb

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.DockerImageName.parse

class InfluxdbContainer : GenericContainer<InfluxdbContainer>
{
    constructor() : super(parse(DEFAULT_IMAGE_NAME))
    {
        setupContainer()
    }

    constructor(dockerImageName: DockerImageName) : super(dockerImageName)
    {
        dockerImageName.assertCompatibleWith(dockerImageName)
        setupContainer()
    }

    private fun setupContainer()
    {
        withExposedPorts(PORT, DOCKER_PORT)
        withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
        withEnv("DOCKER_INFLUXDB_INIT_USERNAME", USERNAME)
        withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", PASSWORD)
        withEnv("DOCKER_INFLUXDB_INIT_ORG", ORGANIZATION)
        withEnv("DOCKER_INFLUXDB_INIT_BUCKET", BUCKET)
    }

    companion object
    {
        private const val DEFAULT_IMAGE_NAME = "influxdb:2"

        const val PORT = 8086
        const val DOCKER_PORT = 8086
        const val USERNAME = "root"
        const val PASSWORD = "root12345678"
        const val ORGANIZATION = "TestOrg"
        const val BUCKET = "TestBucket"
    }
}
