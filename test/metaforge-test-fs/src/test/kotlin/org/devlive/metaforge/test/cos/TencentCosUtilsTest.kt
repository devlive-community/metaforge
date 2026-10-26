package org.devlive.metaforge.test.cos

import org.devlive.metaforge.fs.cos.TencentCosUtils
import org.devlive.metaforge.test.BaseIOUtilsTest

class TencentCosUtilsTest : BaseIOUtilsTest(
    pluginPrefix = "cos",
    ioUtils = TencentCosUtils::class.java
)
