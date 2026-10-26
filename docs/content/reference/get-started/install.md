---
title: 自主机部署
---

MetaForge 是支持用户将服务部署到自主机中。通过本文档用户可以了解如何在自主机中部署 MetaForge。

### 系统要求

---

!!! warning "注意"

    该软件的二进制包基于以下系统进行编译和测试。它尚未在其他版本上进行测试，理论上受支持。

    如果有不支持的系统，请使用源码编译方法主动编译二进制文件。

!!!

| System | Version    |
|--------|------------|
| JDK    | `\>=11`    |
| MySQL  | `\>=5.6.x` |

### 准备安装包

---

!!! info "提示"

    从以下地址下载相应系统的二进制软件包进行安装。如果您需要使用源码安装请前往查看开发者文档模块。

!!!

1.下载最新发布版本

2.将二进制文件下载到本地后运行以下命令

```bash
tar -xvzf metaforge-<VERSION>-bin.tar.gz
```

3. 进入软件根目录

```bash
cd metaforge-<VERSION>
```

### 软件配置

---

对于软件的首次安装，您需要将 `schema/metaforge.sql` 文件中的sql脚本导入MySQL服务器。注意需要导入的脚本根据下载的软件包进行匹配

!!! danger "注意"

    如果您是通过其他版本升级，请执行 `schema/<VERSION>/schema.sql`

!!!

metaforge 软件中的所有配置均在 `configure/application.properties` 文件中。

#### 基本配置

导入 `SQL` 脚本后，修改 `configure/application.properties` 配置文件以修改MySQL服务器的配置信息

```properties
server.port=9096
server.address=localhost
spring.jackson.time-zone=GMT+8
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
metaforge.security.secret=MetaForgeSecretKey
metaforge.security.expiration=86400000
metaforge.editor.sugs.maxSize=1000
```

- `server.port`: 用于配置服务在服务器中启动监听的端口，默认为 `9096`
- `server.address`: 用于配置服务在本地的监听地址，如果需要使用 `IP+端口` 方便外部机器访问，请不要设置为 `localhost`，建议设置为 `0.0.0.0`
- `spring.jackson.time-zone`: 用于配置时区，默认为 `GMT+8`
- `spring.jackson.date-format`: 用于配置日期格式，默认为 `yyyy-MM-dd HH:mm:ss`
- `metaforge.security.secret`: 用于配置数据安全管理的密钥，默认为 `MetaForgeSecretKey`
- `metaforge.security.expiration`: 用于配置数据安全管理的过期时间，单位为毫秒，默认为 `86400000`
- `metaforge.editor.sugs.maxSize`: 用于配置数据编辑器的最大行数，默认为 `1000` 已经失效不在使用

#### Web 服务配置

```properties
spring.mvc.throw-exception-if-no-handler-found=true
spring.resources.add-mappings=false
spring.web.resources.add-mappings=true
```

- `spring.mvc.throw-exception-if-no-handler-found`: 用于配置是否抛出异常
- `spring.resources.add-mappings`: 用于配置是否启用静态资源映射
- `spring.web.resources.add-mappings`: 用于配置是否启用静态资源映射

#### 数据库配置

!!! danger "注意"

    如果版本 `>=8.x`，请设置 `allowPublicKeyRetrieval=true`

!!!

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/metaforge?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true&useSSL=false&useOldAliasMetadataBehavior=true&jdbcCompliantTruncation=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=12345678
```

- `spring.datasource.url`: 用于配置数据库连接地址
- `spring.datasource.username`: 用于配置数据库用户名
- `spring.datasource.password`: 用于配置数据库密码

!!! info "提示"

    支持所有 Spring Data 的配置参数

!!!

#### 执行器配置

从 `2026.0.0` 起，执行器配置（原 `metaforge.executor.*`）已经迁移到数据库表 `metaforge_configure`（category = `EXECUTOR`），不再通过 `application.properties` 配置。

- 默认值由各执行器插件（`ExecutorPlugin.configures()`）声明，**首次启动会自动写入数据库**
- 管理员可在 UI 的 **系统配置 → 运行时配置** 页面（`/system/configure`）修改全部字段
- 普通用户在触发同步时，可对每个执行器的 `tunable=true` 字段做**本次运行临时覆盖**（不持久化）
- 每次同步实际生效的配置 JSON 会写入对应 `metaforge_dataset_history.executor_configure`，方便审计

#### 上传配置

```properties
metaforge.config.data=
metaforge.cache.data=
```

- `metaforge.config.data`: 用于配置上传配置文件的路径
- `metaforge.cache.data`: 用于配置上传缓存文件的路径

#### OpenAi 配置

```properties
metaforge.openai.backend=https://api.openai.com
metaforge.openai.token=
metaforge.openai.model=GPT_35_TURBO_0613
metaforge.openai.timeout=30
```

- `metaforge.openai.backend`: 用于配置 OpenAI 的后端地址
- `metaforge.openai.token`: 用于配置 OpenAI 的 token
- `metaforge.openai.model`: 用于配置 OpenAI 的模型
- `metaforge.openai.timeout`: 用于配置 OpenAI 的超时时间，单位为秒

#### 主系统配置

```properties
metaforge.registration.enable=
metaforge.captcha.enable=
metaforge.cache.maximum=100000
metaforge.cache.expiration=5
metaforge.audit.sql.print=false
```

- `metaforge.registration.enable`: 用于配置是否开启注册
- `metaforge.captcha.enable`: 用于配置是否开启验证码
- `metaforge.cache.maximum`: 用于配置缓存最大值
- `metaforge.cache.expiration`: 用于配置缓存过期时间，单位为分钟
- `metaforge.audit.sql.print`: 用于配置是否打印 SQL

#### 流水线配置

```properties
metaforge.pipeline.maxRunning=100
metaforge.pipeline.maxQueue=200
metaforge.pipeline.reset=STOPPED
```

- `metaforge.pipeline.maxRunning`: 用于配置最大运行数
- `metaforge.pipeline.maxQueue`: 用于配置最大队列
- `metaforge.pipeline.reset`: 用于配置重置策略

#### 存储配置

---

支持的存储类型详见 https://github.com/devlive-community/metaforge/tree/dev/fs

##### 本地存储配置

```properties
metaforge.fs.type=Local
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: 用于配置文件系统类型
- `metaforge.fs.access`: 用于配置文件系统访问，该类型可以为空
- `metaforge.fs.secret`: 用于配置文件系统密钥，该类型可以为空
- `metaforge.fs.endpoint`: 用于配置文件系统端点，如果填写后将追加为目录 **该类型可以为空**
- `metaforge.fs.bucket`: 用于配置文件系统存储桶，该类型可以为空

##### 阿里云 OSS 配置

```properties
metaforge.fs.type=AliOss
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: AliOss

##### 七牛云配置

```properties
metaforge.fs.type=Qiniu
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: Qiniu

#### 实验性功能

```properties
metaforge.experimental.autoLimit=true
metaforge.experimental.data={user.dir}/data
metaforge.experimental.avatarPath={username}/avatar/
```

- `metaforge.experimental.autoLimit`: 用于配置是否自动增加 LIMIT
- `metaforge.experimental.data`: 用于配置实验性功能的数据路径
- `metaforge.experimental.avatarPath`: 用于配置实验性功能的头像路径

#### 日志配置

!!! warning "警告"

    如果需要修改日志配置，只需修改 `configure/logback.xml` 配置文件即可

!!!

#### JVM 配置

!!! warning "警告"

    如果您需要定制化 JVM 配置，只需修改 `configure/jvm.conf` 配置文件即可

!!!

#### 插件管理器配置

```properties
plugin.manager.extend.packages=com.fasterxml.jackson
```

- `plugin.manager.extend.packages`: 用于配置插件扩展包，配置后将优先加载父类加载器中的依赖

### 软件启动

---

> 启动服务前请安装系统需要的各种插件，执行命令 `./bin/install-plugin.sh`，也可以到服务商店中进行安装。

#### 启动服务

MetaForge服务启动非常简单，执行以下脚本

```bash
./bin/startup.sh
```

#### 停止服务

停止服务并执行以下脚本

```bash
./bin/shutdown.sh
```

#### 调试服务

!!! info "提示"

    如果要调试系统，可以使用 `./bin/debug.sh` 启动服务，但关闭窗口时它将停止

!!!