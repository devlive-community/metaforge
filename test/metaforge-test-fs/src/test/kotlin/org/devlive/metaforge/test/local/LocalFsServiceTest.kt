package org.devlive.metaforge.test.local

import org.devlive.metaforge.test.BaseServiceTest

class LocalFsServiceTest : BaseServiceTest(
    pluginName = "LocalFs",
    pluginHome = "fs/metaforge-fs-local",
    pluginPrefix = "local"
)
