---
title: Local (本地)
---

在 MetaForge 中目前已经支持 Local，支持功能如下：

- 上传文件
- 预览&下载文件
- 删除文件

### MetaForge 使用配置

---

在 MetaForge 中文件系统用于配置系统的缓冲以及临时文件的存储。修改 `application.properties` 文件如下内容：

```properties
metaforge.fs.type=Local
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: 文件系统的类型只能是 **Local**
- `metaforge.fs.access`: 不做任何设置
- `metaforge.fs.secret`: 不做任何设置
- `metaforge.fs.endpoint`: 不做任何设置
- `metaforge.fs.bucket`: 如果设置则为目录的名称，不设置则不做任何设置

### 依赖使用方式

---

该插件支持第三方依赖方式引入，引入依赖如下：

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-fs-local</artifactId>
    <version>${VERSION}</version>
</dependency>
```

使用代码如下：

```java
package org.devlive.metaforge.fs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class LocalFsTest
{
    private Injector injector;
    private FsRequest request;

    @Before
    public void before()
    {
        injector = Guice.createInjector(new FsManager());
        request = FsRequest.builder()
            .access(null)
            .secret(null)
            .endpoint(String.join(File.separator, System.getProperty("user.dir"), "data"))
            .bucket("tmp")
            .localPath(String.join(File.separator, System.getProperty("user.dir"), "src/main/java/org/devlive/metaforge/fs/LocalFs.java"))
            .fileName("LocalFs.java")
            .build();
    }

    @Test
    public void test()
    {
        Set<Fs> sets = injector.getInstance(Key.get(new TypeLiteral<Set<Fs>>() {}));
        Assert.assertEquals("Local", sets.stream().findFirst().get().name());
    }

    @Test
    public void writer()
    {
        Optional<Fs> optional = injector.getInstance(Key.get(new TypeLiteral<Set<Fs>>() {})).stream().findFirst();
        if (optional.isPresent()) {
            Assert.assertEquals("Local", optional.get().name());
        }

        FsResponse response = optional.get().writer(request);
        Assert.assertEquals(true, response.isSuccessful());
    }

    @Test
    public void reader()
    {
        Optional<Fs> optional = injector.getInstance(Key.get(new TypeLiteral<Set<Fs>>() {})).stream().findFirst();
        if (optional.isPresent()) {
            Assert.assertEquals("Local", optional.get().name());
        }

        FsResponse response = optional.get().reader(request);
        Assert.assertEquals(true, response.isSuccessful());

        log.info("====== [ {} ] ======", response.getRemote());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getContext(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }
            catch (IOException e) {
                log.error("Reader error", e);
            }
        }

    @Test
    public void testDelete()
    {
        injector.getInstance(Key.get(new TypeLiteral<Set<Fs>>() {}))
            .stream()
            .findFirst()
            .ifPresent(it -> {
        FsResponse response = it.delete(request);
        Assert.assertTrue(response.isSuccessful());
    });
    }
}
```
