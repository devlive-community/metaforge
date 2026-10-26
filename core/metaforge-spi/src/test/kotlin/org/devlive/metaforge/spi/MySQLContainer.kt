package org.devlive.metaforge.spi

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class MySQLContainer : GenericContainer<MySQLContainer> {
    constructor() : super(DockerImageName.parse(IMAGE_MYSQL_80)) {
        setupContainer()
    }

    constructor(dockerImageName: DockerImageName) : super(dockerImageName) {
        dockerImageName.assertCompatibleWith(dockerImageName)
        setupContainer()
    }

    private fun setupContainer() {
        withExposedPorts(PORT, 3306)
        withEnv("MYSQL_ROOT_PASSWORD", PASSWORD)
    }

    companion object {
        const val IMAGE_MYSQL_80 = "mysql:8.2.0"
        const val PORT = 13306
        const val PASSWORD = "community@Devlive.Org"
    }
}
