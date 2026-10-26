---
title: Deploying in Self Host
---

MetaForge enables users to deploy services to their own machines. This document allows users to learn how to deploy MetaForge in an autonomous machine.

### System Requirements

---

!!! warning "Warning"

    The binary package of the software is compiled and tested based on the following system. It has not been tested on other versions and is theoretically supported.

    If you have an unsupported system, use the source code compilation method to actively compile the binary file.

!!!

| System | Version    |
|--------|------------|
| JDK    | `\>=11`    |
| MySQL  | `\>=5.6.x` |

### Prepare the installation package

---

!!! info "Info"

    Download the binary package of the corresponding system from the following address and install it. If you need to install using source code, go to the Developer Documentation module.

!!!

1. [Download the latest release version](../../download.md)

2. Run the following command after downloading the binaries locally
```bash
tar -xvzf metaforge-release.tar.gz
```
3. Go to the root directory of the software
```bash
cd metaforge
```

### Software configuration

---

For the first installation of the software, you need to import the sql script from the `schema/metaforge.sql` file into the MySQL server. Note: The scripts that need to be imported are matched based on the downloaded package

!!! danger "Warning"

    If you are upgrading from another version, run `schema/VERSION/schema.sql`

!!!

All configurations in the MetaForge software are in the `configure/application.properties` file.

#### Basic configuration

After importing the `SQL` script, modify the `configure/application.properties` configuration file to modify the configuration information of the MySQL server

```properties
server.port=9096
server.address=localhost
# Fixed serialized data missing for 8 hours
spring.jackson.time-zone=GMT+8
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
# metaforge security management configuration
metaforge.security.secret=MetaForgeSecretKey
metaforge.security.expiration=86400000
# metaforge editor configuration
metaforge.editor.sugs.maxSize=1000
```

- `server.port`: The port on which the service initiates the listener on the server
- `server.address`: Configure the local listening address of the service
- `spring.jackson.time-zone`: Used to configure the time zone
- `spring.jackson.date-format`: Lets you configure the date format
- `metaforge.security.secret`: The key used to configure data security management
- `metaforge.security.expiration`: The expiration time for configuring data security management
- `metaforge.editor.sugs.maxSize`: Lets you configure the maximum number of rows in the data editor

#### Web service configuration

```properties
spring.mvc.throw-exception-if-no-handler-found=true
spring.resources.add-mappings=false
spring.web.resources.add-mappings=true
```

- `spring.mvc.throw-exception-if-no-handler-found`: Used to configure whether an exception is thrown
- `spring.resources.add-mappings`: Lets you configure whether to enable static resource mapping
- `spring.web.resources.add-mappings`: Lets you configure whether to enable static resource mapping

#### Database configuration

!!! danger "Warning"

    If version `>=8.x`, set `allowPublicKeyRetrieval=true`

!!!

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/metaforge?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true&useSSL=false&useOldAliasMetadataBehavior=true&jdbcCompliantTruncation=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=12345678
```

- `spring.datasource.url`: Lets you configure the database connection address
- `spring.datasource.username`: Lets you configure the database username
- `spring.datasource.password`: Used to configure the database password

!!! info "Info"

    All Spring Data configuration parameters are supported

!!!

#### Actuator configuration

Since `2026.0.0`, executor configuration (formerly `metaforge.executor.*`) has moved to the database table `metaforge_configure` (category = `EXECUTOR`) and is no longer set via `application.properties`.

- Default values are declared by each executor plugin (`ExecutorPlugin.configures()`) and **auto-seeded on first startup**.
- Administrators can edit every field in the UI under **System Configuration → Runtime Configuration** (`/system/configure`).
- Regular users can override `tunable=true` fields **per sync invocation** (not persisted).
- The effective configuration JSON used by each sync is recorded in `metaforge_dataset_history.executor_configure` for audit.

#### Upload the configuration

```properties
metaforge.config.data=
metaforge.cache.data=
```

- `metaforge.config.data`: The path used to configure the upload profile
- `metaforge.cache.data`: Lets you configure the path to upload the cache file

#### OpenAi configuration

```properties
metaforge.openai.backend=https://api.openai.com
metaforge.openai.token=
metaforge.openai.model=GPT_35_TURBO_0613
metaforge.openai.timeout=30
```

- `metaforge.openai.backend`: Used to configure OpenAI's backend address
- `metaforge.openai.token`: The token used to configure OpenAI
- `metaforge.openai.model`: The model used to configure OpenAI
- `metaforge.openai.timeout`: Used to configure the timeout period for OpenAI

#### Primary system configuration

```properties
metaforge.registration.enable=
metaforge.captcha.enable=
metaforge.cache.maximum=100000
metaforge.cache.expiration=5
metaforge.audit.sql.print=false
```

- `metaforge.registration.enable`: Used to configure whether to enable registration
- `metaforge.captcha.enable`: This parameter is used to configure whether to enable the verification code
- `metaforge.cache.maximum`: Lets you configure the maximum cache value
- `metaforge.cache.expiration`: Lets you configure the cache expiration time
- `metaforge.audit.sql.print`: This parameter is used to configure whether to print SQL statements

#### Pipeline configuration

```properties
metaforge.pipeline.maxRunning=100
metaforge.pipeline.maxQueue=200
metaforge.pipeline.reset=STOPPED
```

- `metaforge.pipeline.maxRunning`: Used to configure the maximum number of runs
- `metaforge.pipeline.maxQueue`: Used to configure the maximum queue
- `metaforge.pipeline.reset`: Lets you configure a reset policy

#### Storage configuration

---

Storage currently supports

- `Local`: local storage
- `AliOss`: Alibaba Cloud OSS
- `Qiniu`: Qiniu Cloud

##### Local storage configuration

```properties
metaforge.fs.type=Local
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: used to configure the file system type
- `metaforge.fs.access`: used to configure file system access, this type can be empty
- `metaforge.fs.secret`: used to configure the file system key, this type can be empty
- `metaforge.fs.endpoint`: used to configure the file system endpoint. If filled in, it will be appended as a directory **This type can be empty**
- `metaforge.fs.bucket`: used to configure the file system bucket, this type can be empty

##### Alibaba Cloud OSS configuration

```properties
metaforge.fs.type=AliOss
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: AliOss

##### Qiniu Cloud Configuration

```properties
metaforge.fs.type=Qiniu
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: Qiniu

#### Experimental features

```properties
metaforge.experimental.autoLimit=true
metaforge.experimental.data={user.dir}/data
metaforge.experimental.avatarPath={username}/avatar/
```

- `metaforge.experimental.autoLimit`: This parameter specifies whether to automatically increase the LIMIT
- `metaforge.experimental.data`: The data path used to configure the experimental feature
- `metaforge.experimental.avatarPath`: Avatar path to configure experimental features

#### Log configuration

!!! warning

    If you need to modify the log configuration, you can simply modify the `configure/logback.xml` configuration file

#### JVM configuration

!!! warning "Warning"

    If you need to customize the JVM configuration, you can simply modify the `configure/jvm.conf` configuration file

!!!

### Software startup

---

> Before starting the service, please install various plug-ins required by the system and execute the command `./bin/install-plugin.sh`

#### Start the service

MetaForge service startup is very simple, execute the following script

```bash
./bin/startup.sh
```

#### Discontinuation of service

Stop the service and execute the following script

```bash
./bin/shutdown.sh
```

#### Debug the service

!!! info "Info"

    If you want to debug the system, you can start the service with `./bin/debug.sh`, but it stops when you close the window

!!!