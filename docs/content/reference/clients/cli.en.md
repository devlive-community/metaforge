---
title: Command line interface
---

The MetaForge CLI provides a terminal-based, interactive shell for running queries. The CLI is a self-executing JAR file, which means it acts like a normal UNIX executable.

### Requirements

---

The CLI requires a Java virtual machine available on the path. It can be used with Java version 8 and higher.

The CLI uses the MetaForge client REST API over HTTP/HTTPS to communicate with the system.

The CLI version should be identical to the version of system, or newer.

### Installation

---

Download [metaforge-client-cli-1.6.0.jar](https://repo1.maven.org/maven2/org/devlive/metaforge/metaforge-client-cli/1.6.0/metaforge-client-cli-1.6.0.jar), rename it to metaforge, make it executable with `chmod +x`.

### Running the CLI

---

```bash
./metaforge

connect -h 127.0.0.1 -p 9096 -u username -P password
```

If successful, you will get a prompt to execute commands. Use the `help` command to see a list of supported commands.

| Command                       | Description                                                           |
|-------------------------------|-----------------------------------------------------------------------|
| `source info`                 | Get data source details                                               |
| `source list`                 | Get a list of remote server data sources                              |
| `source use <SourceID>`       | Set the data source flag for subsequent operations on the data source |
| `source execute "<QuerySQL>"` | Execute remote SQL                                                    |
