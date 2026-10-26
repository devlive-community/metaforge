package org.devlive.metaforge.plugin.solr

import org.devlive.metaforge.plugin.BaseContainer
import org.testcontainers.utility.DockerImageName

class SolrContainer : BaseContainer<SolrContainer>
{
    companion object
    {
        const val DEFAULT_IMAGE_NAME = "solr:9.6.1-slim"
        const val SOLR_PORT = "SOLR_PORT"
    }

    constructor() : super(
        imageName = DEFAULT_IMAGE_NAME,
        ports = mapOf(SOLR_PORT to 8983)
    )

    constructor(dockerImageName: DockerImageName) : super(
        dockerImageName = dockerImageName,
        ports = mapOf(SOLR_PORT to 8983)
    )
}
