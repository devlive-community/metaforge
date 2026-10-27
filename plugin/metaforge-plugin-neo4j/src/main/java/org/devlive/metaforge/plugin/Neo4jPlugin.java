package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Neo4jPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        return PluginConfigure.builder()
                .name("Neo4j")
                .configures(Arrays.asList(
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.name)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .message("name is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.host)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("127.0.0.1")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.port)
                                .type(PluginConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(7687)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.username)
                                .type(PluginConfigureFieldType.String)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.password)
                                .type(PluginConfigureFieldType.String)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("neo4j")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build()))
                .descriptions(Map.of(
                        "zh", "Neo4j 是领先的图数据库，以节点和关系原生存储与查询高度关联的数据。",
                        "en", "Neo4j is the leading graph database, storing and querying highly connected data with native nodes and relationships."))
                .features(Map.of(
                        "zh", List.of("原生图存储", "Cypher 查询语言", "ACID 事务", "高性能图遍历"),
                        "en", List.of("Native graph storage", "Cypher query language", "ACID transactions", "High-performance traversals")))
                .scenes(Map.of(
                        "zh", List.of("知识图谱", "社交网络", "反欺诈风控", "推荐系统"),
                        "en", List.of("Knowledge graphs", "Social networks", "Fraud detection", "Recommendation engines")))
                .category(PluginConfigureCategory.NOSQL)
                .build();
    }
}
