package org.devlive.metaforge.test.s3

import org.devlive.metaforge.fs.s3.AmazonS3Utils
import org.devlive.metaforge.test.BaseIOUtilsTest

class AmazonS3UtilsTest : BaseIOUtilsTest(
    pluginPrefix = "s3",
    ioUtils = AmazonS3Utils::class.java
)
