package org.devlive.metaforge.test.alioss

import org.devlive.metaforge.fs.alioss.IOUtils
import org.devlive.metaforge.test.BaseIOUtilsTest

class IOUtilsTest : BaseIOUtilsTest(
    pluginPrefix = "alioss",
    ioUtils = IOUtils::class.java
)
