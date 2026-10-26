/**
 * 数据源目录元数据（纯前端展示信息）：
 * - category: 原型左侧分类
 * - desc/features/scenes: 原型右侧详情面板文案（zh/en）
 * 未登记的数据源走 genericFallback（分类「其他」）。
 */
export type SourceCategoryKey =
  | 'all'
  | 'rdb'
  | 'bigdata'
  | 'cloud'
  | 'nosql'
  | 'file'
  | 'api'
  | 'mq'
  | 'other'

export interface SourceCatalogEntry
{
    category: Exclude<SourceCategoryKey, 'all'>
    popular?: boolean
    desc: { zh: string; en: string }
    features?: { zh: string[]; en: string[] }
    scenes?: { zh: string[]; en: string[] }
}


const entry = (
    category: SourceCatalogEntry['category'],
    desc: { zh: string; en: string },
    features?: { zh: string[]; en: string[] },
    scenes?: { zh: string[]; en: string[] },
    popular?: boolean): SourceCatalogEntry => ({
    category,
    popular,
    desc,
    features,
    scenes
})

const f = (zh: string[], en: string[]) => ({ zh, en })

export const SOURCE_CATALOG: Record<string, SourceCatalogEntry> = {
    mysql: entry('rdb',
        { zh: 'MySQL 是最流行的开源关系型数据库，适用于各类业务系统的数据存储。', en: 'MySQL is the most popular open-source relational database for all kinds of business systems.' },
        f(['开源免费', '高性能和稳定性', '广泛的社区支持', '支持大规模数据处理'], ['Open source', 'High performance and stability', 'Wide community support', 'Large-scale data processing']),
        f(['业务系统', '数据仓库', '数据分析', '报表系统', '实时数据同步'], ['Business systems', 'Data warehouse', 'Data analysis', 'Reporting', 'Real-time sync']),
        true),
    postgresql: entry('rdb',
        { zh: 'PostgreSQL 是功能最强大的开源关系型数据库，以标准兼容与扩展性著称。', en: 'PostgreSQL is the most advanced open-source relational database, known for standards compliance and extensibility.' },
        f(['标准 SQL 兼容', '丰富的数据类型', '强大的扩展生态', '事务与并发控制'], ['Standard SQL', 'Rich data types', 'Extensible ecosystem', 'Transactions and concurrency']),
        f(['业务系统', '数据分析', '地理信息', '数据仓库'], ['Business systems', 'Data analysis', 'GIS', 'Data warehouse']),
        true),
    oracle: entry('rdb',
        { zh: 'Oracle 数据库是企业级关系型数据库的代表，广泛应用于金融、电信等行业。', en: 'Oracle Database is the leading enterprise relational database, widely used in finance and telecom.' },
        f(['企业级稳定性', '强大的 PL/SQL', '高可用架构', '完善的安全体系'], ['Enterprise stability', 'Powerful PL/SQL', 'High availability', 'Mature security']),
        f(['金融行业', '电信行业', '大型核心系统', '数据仓库'], ['Finance', 'Telecom', 'Large core systems', 'Data warehouse'])),
    'microsoft sql server': entry('rdb',
        { zh: 'Microsoft SQL Server 是微软的企业级数据库，与 .NET 生态深度集成。', en: 'Microsoft SQL Server is Microsoft’s enterprise database, deeply integrated with the .NET ecosystem.' },
        f(['与 Windows/.NET 集成', '强大的 BI 工具', '企业级安全', '易于管理'], ['Windows/.NET integration', 'Powerful BI tooling', 'Enterprise security', 'Easy management']),
        f(['业务系统', '报表分析', '数据仓库'], ['Business systems', 'Reporting and analytics', 'Data warehouse'])),
    mariadb: entry('rdb',
        { zh: 'MariaDB 是 MySQL 的开源分支，保持兼容并持续引入新特性。', en: 'MariaDB is an open-source MySQL fork, keeping compatibility while adding new features.' },
        f(['MySQL 兼容', '开源社区驱动', '多存储引擎', '高性能'], ['MySQL compatible', 'Community driven', 'Multiple storage engines', 'High performance']),
        f(['业务系统', '数据存储', '互联网应用'], ['Business systems', 'Data storage', 'Web applications'])),
    mongodb: entry('nosql',
        { zh: 'MongoDB 是最流行的文档型 NoSQL 数据库，适合灵活多变的数据结构。', en: 'MongoDB is the most popular document-oriented NoSQL database for flexible data structures.' },
        f(['文档模型', '水平扩展', '丰富的查询能力', '高可用副本集'], ['Document model', 'Horizontal scaling', 'Rich queries', 'Replica sets']),
        f(['内容管理', '物联网', '用户画像', '实时数据'], ['Content management', 'IoT', 'User profiles', 'Real-time data'])),
    redis: entry('nosql',
        { zh: 'Redis 是高性能的内存键值数据库，广泛用于缓存与消息场景。', en: 'Redis is a high-performance in-memory key-value store, widely used for caching and messaging.' },
        f(['亚毫秒级读写', '丰富数据结构', '持久化支持', '主从与集群'], ['Sub-millisecond R/W', 'Rich data structures', 'Persistence', 'Replication and cluster']),
        f(['缓存加速', '会话存储', '排行榜', '消息队列'], ['Caching', 'Session storage', 'Leaderboards', 'Message queue'])),
    elasticsearch: entry('nosql',
        { zh: 'Elasticsearch 是分布式搜索与分析引擎，擅长全文检索与日志分析。', en: 'Elasticsearch is a distributed search and analytics engine for full-text search and log analytics.' },
        f(['全文检索', '分布式架构', '实时分析', '丰富的生态'], ['Full-text search', 'Distributed', 'Real-time analytics', 'Rich ecosystem']),
        f(['日志分析', '全文搜索', '监控告警', '业务检索'], ['Log analytics', 'Full-text search', 'Monitoring', 'Business search'])),
    clickhouse: entry('bigdata',
        { zh: 'ClickHouse 是高性能的列式 OLAP 数据库，适合海量数据的实时分析。', en: 'ClickHouse is a high-performance columnar OLAP database for real-time analytics on massive data.' },
        f(['列式存储', '极致查询性能', '数据压缩', '实时写入'], ['Columnar storage', 'Blazing queries', 'Compression', 'Real-time ingestion']),
        f(['用户行为分析', '监控大盘', '商业智能', '日志分析'], ['Behavior analytics', 'Monitoring dashboards', 'BI', 'Log analytics']),
        true),
    hive: entry('bigdata',
        { zh: 'Hive 是基于 Hadoop 的数据仓库工具，适合大规模数据的离线分析。', en: 'Hive is a Hadoop-based data warehouse tool for offline analytics on massive data.' },
        f(['SQL on Hadoop', '海量数据', '生态成熟', '离线批处理'], ['SQL on Hadoop', 'Massive scale', 'Mature ecosystem', 'Batch processing']),
        f(['离线分析', '数据仓库', 'ETL 处理'], ['Offline analytics', 'Data warehouse', 'ETL'])),
    hbase: entry('bigdata',
        { zh: 'HBase 是构建在 Hadoop 上的分布式列式数据库，适合海量稀疏数据。', en: 'HBase is a distributed columnar database on Hadoop for massive sparse data.' },
        f(['亿级行读写', '强一致性', '与 Hadoop 集成', '水平扩展'], ['Billions of rows', 'Strong consistency', 'Hadoop integration', 'Horizontal scaling']),
        f(['消息存储', '画像数据', '风控数据'], ['Message storage', 'Profile data', 'Risk data'])),
    kafka: entry('mq',
        { zh: 'Kafka 是高吞吐的分布式消息队列，是流式数据管道的事实标准。', en: 'Kafka is a high-throughput distributed message queue, the de-facto standard for streaming pipelines.' },
        f(['高吞吐低延迟', '分布式与可扩展', '持久化存储', '流式生态'], ['High throughput', 'Distributed and scalable', 'Durable storage', 'Streaming ecosystem']),
        f(['日志采集', '流式计算', '事件驱动', '数据集成'], ['Log collection', 'Stream processing', 'Event-driven', 'Data integration']),
        true),
    spark: entry('bigdata',
        { zh: 'Spark 是通用的大数据计算引擎，支持批处理、流处理与机器学习。', en: 'Spark is a general-purpose big data engine supporting batch, streaming and machine learning.' },
        f(['内存计算', '一站式引擎', '丰富的 API', '与 Hadoop 集成'], ['In-memory compute', 'Unified engine', 'Rich APIs', 'Hadoop integration']),
        f(['离线计算', '实时计算', '机器学习'], ['Batch compute', 'Stream compute', 'Machine learning'])),
    starrocks: entry('bigdata',
        { zh: 'StarRocks 是高性能的实时分析型数据库，支持极速的多维分析。', en: 'StarRocks is a high-performance real-time analytical database for blazing-fast OLAP.' },
        f(['实时分析', '高并发查询', 'join 性能强', '兼容 MySQL 协议'], ['Real-time analytics', 'High concurrency', 'Strong joins', 'MySQL protocol']),
        f(['实时报表', '用户画像', '湖仓分析'], ['Real-time reporting', 'User profiles', 'Lakehouse analytics'])),
    doris: entry('bigdata',
        { zh: 'Doris 是易用的实时分析数据库，支持高并发点查与复杂分析。', en: 'Doris is an easy-to-use real-time analytical database supporting both point queries and complex analytics.' },
        f(['简单易用', '高并发', '实时更新', 'MySQL 协议'], ['Easy to use', 'High concurrency', 'Real-time updates', 'MySQL protocol']),
        f(['报表分析', '用户行为', '数据湖'], ['Reporting', 'User behavior', 'Data lake'])),
    snowflake: entry('cloud',
        { zh: 'Snowflake 是云原生数据仓库，按需弹性扩展、免运维。', en: 'Snowflake is a cloud-native data warehouse with elastic scaling and zero maintenance.' },
        f(['云原生', '存算分离', '弹性扩缩容', '免运维'], ['Cloud native', 'Storage/compute separation', 'Elastic scaling', 'Zero maintenance']),
        f(['数据仓库', '数据分析', '数据共享'], ['Data warehouse', 'Data analysis', 'Data sharing'])),
    bigquery: entry('cloud',
        { zh: 'BigQuery 是谷歌云的无服务器数据仓库，支持超大规模数据分析。', en: 'BigQuery is Google Cloud’s serverless data warehouse for analytics at any scale.' },
        f(['无服务器', '极速查询', '按量付费', '与 GCP 集成'], ['Serverless', 'Fast queries', 'Pay as you go', 'GCP integration']),
        f(['数据分析', '机器学习', '商业智能'], ['Data analysis', 'Machine learning', 'BI'])),
    'amazon s3': entry('cloud',
        { zh: 'Amazon S3 是业界标准的对象存储服务，提供 99.999999999% 的持久性。', en: 'Amazon S3 is the industry-standard object storage with 11 nines of durability.' },
        f(['高持久性', '无限容量', '丰富的生态', '生命周期管理'], ['High durability', 'Unlimited capacity', 'Rich ecosystem', 'Lifecycle management']),
        f(['数据湖', '备份归档', '静态资源'], ['Data lake', 'Backup and archive', 'Static assets'])),
    'azure blob': entry('cloud',
        { zh: 'Azure Blob 是微软云的对象存储服务，适合云原生工作负载。', en: 'Azure Blob Storage is Microsoft Cloud’s object storage for cloud-native workloads.' },
        f(['Azure 集成', '分层存储', '高可用', '安全合规'], ['Azure integration', 'Tiered storage', 'High availability', 'Security and compliance']),
        f(['数据湖', '备份归档', '媒体存储'], ['Data lake', 'Backup and archive', 'Media storage'])),
    'ftp/sftp': entry('file',
        { zh: 'FTP/SFTP 是经典的文件传输协议，广泛用于系统间的文件交换。', en: 'FTP/SFTP are classic file transfer protocols widely used for file exchange between systems.' },
        f(['协议通用', '部署简单', '广泛支持', '跨平台'], ['Universal protocol', 'Easy deployment', 'Widely supported', 'Cross-platform']),
        f(['文件交换', '报表传输', '数据导入'], ['File exchange', 'Report delivery', 'Data import'])),
    '本地文件': entry('file',
        { zh: '本地文件数据源支持读取服务器本地的 CSV、XML 等文件。', en: 'The local file source reads CSV, XML and other files from the server.' },
        f(['无需网络', '简单直接', '支持多格式'], ['No network needed', 'Simple and direct', 'Multiple formats']),
        f(['本地导入', '临时分析', '数据迁移'], ['Local import', 'Ad-hoc analysis', 'Data migration'])),
    'rest api': entry('api',
        { zh: 'Rest API 数据源可以对接任意 HTTP 接口，把 Web 数据当作数据源使用。', en: 'The Rest API source connects to any HTTP endpoint and treats web data as a data source.' },
        f(['通用性强', '实时获取', '灵活配置'], ['Universal', 'Real-time fetch', 'Flexible configuration']),
        f(['Web 数据', '第三方服务', '实时接口'], ['Web data', 'Third-party services', 'Real-time APIs'])),
    钉钉: entry('api',
        { zh: '钉钉数据源用于读取钉钉开放平台的企业通讯与审批数据。', en: 'The DingTalk source reads organization and approval data from the DingTalk open platform.' },
        f(['企业通讯录', '审批数据', '与钉钉集成'], ['Organization directory', 'Approval data', 'DingTalk integration']),
        f(['组织管理', '审批同步'], ['Organization', 'Approval sync'])),
    企业微信: entry('api',
        { zh: '企业微信数据源用于读取企业微信的通讯与业务数据。', en: 'The WeCom source reads organization and business data from WeCom.' },
        f(['企业通讯录', '业务数据', '与企业微信集成'], ['Organization directory', 'Business data', 'WeCom integration']),
        f(['组织管理', '消息同步'], ['Organization', 'Message sync']))
}

/** 未登记数据源的兜底描述 */
export const genericFallback = (name: string, pluginDescription?: string): SourceCatalogEntry => ({
    category: 'other',
    desc: {
        zh: pluginDescription || `${ name } 数据源连接。`,
        en: pluginDescription || `${ name } data source connection.`
    }
})

/** 按插件名查找目录信息（精确 → 包含） */
export const findCatalogEntry = (name: string): SourceCatalogEntry => {
    const key = (name || '').toLowerCase()
    if (SOURCE_CATALOG[key]) {
        return SOURCE_CATALOG[key]
    }
    const contained = Object.keys(SOURCE_CATALOG).find(k => key.includes(k) || k.includes(key))
    return contained ? SOURCE_CATALOG[contained] : genericFallback(name)
}
