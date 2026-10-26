package org.devlive.metaforge.test.s3

import org.devlive.metaforge.test.BaseServiceTest

class AmazonS3FsServiceTest : BaseServiceTest(
    pluginName = "AmazonS3Fs",
    pluginHome = "fs/metaforge-fs-amazon-s3",
    pluginPrefix = "s3"
)
