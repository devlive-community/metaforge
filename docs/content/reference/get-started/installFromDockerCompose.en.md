---
title: Deploying in Docker Compose
---

The MetaForge project provides Docker Compose deployment by downloading the [docker-compose.yml](https://github.com/devlive-community/metaforge/blob/dev/docker-compose.yml) file, or using the following code for service deploy.

#### Simplified version

---

> Only some basic functions

```yaml
version: '3.3'

services:
  app-mysql:
    image: mysql:latest
    environment:
      MYSQL_ROOT_PASSWORD: 12345678
      MYSQL_DATABASE: metaforge
    ports:
      - "3306:3306"
    volumes:
      - ./configure/schema/metaforge.sql:/docker-entrypoint-initdb.d/schema.sql

  app-metaforge:
    image: devliveorg/metaforge:latest
    restart: always
    ports:
      - "9099:9099"
    depends_on:
      - app-mysql
      - app-clickhouse
    volumes:
      - ./configure/docker/application.properties:/opt/app/metaforge/configure/application.properties
```

#### Advanced version

---

> This method includes the `dataset` function

```yaml
version: '3.3'

services:
  app-mysql:
    image: mysql:latest
    environment:
      MYSQL_ROOT_PASSWORD: 12345678
      MYSQL_DATABASE: metaforge
    ports:
      - "13306:3306"
    volumes:
      - ./configure/schema/metaforge.sql:/docker-entrypoint-initdb.d/schema.sql
      - mysql_data:/var/lib/mysql

  app-clickhouse:
    image: clickhouse/clickhouse-server:latest
    restart: always
    ports:
      - "8123:8123"
    environment:
      - CLICKHOUSE_DB=metaforge
      - CLICKHOUSE_USER=default
      - CLICKHOUSE_PASSWORD=da39a3ee5e6b4b0d3255bfef95601890afd80709
    volumes:
      - clickhouse_data:/var/lib/clickhouse
      - clickhouse_logs:/var/log/clickhouse-server

  app-metaforge:
    image: devliveorg/metaforge:latest
    restart: always
    ports:
      - "9099:9096"
    depends_on:
      - app-mysql
      - app-clickhouse
    volumes:
      - ./configure/docker/application.properties:/opt/app/metaforge/configure/application.properties
      - metaforge_data:/opt/app/metaforge/data

volumes:
  mysql_data:
  clickhouse_data:
  clickhouse_logs:
  metaforge_data:
```

!!! warning "Warning"

    You need to download multiple files at the same time:

    - [metaforge.sql](https://github.com/devlive-community/metaforge/blob/dev/configure/schema/metaforge.sql)
    - [application.properties](https://github.com/devlive-community/metaforge/blob/dev/configure/docker/application.properties)

    After the download is completed, place them in the specified directory, that is, `./configure/docker/` and `./configure/schema/`. If you need to customize the directory, you can modify the ` mounted in the `docker-compose.yml` file. volumes` can be configured.

!!!

## Start service

---

After the above work is completed, use the following command to start the service. **Must be executed in the directory containing the docker-compose.yml file**

```bash
docker-compose up
```

If you need to start in the background, use the following command

```bash
docker-compose up -d
```
After successful startup, open http://localhost:9096/ in the browser to see the website.

## Out of service

---

To stop the service you need to use the following command

```bash
docker-compose down
```
