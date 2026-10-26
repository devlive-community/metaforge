-- MySQL dump 10.13  Distrib 8.3.0, for macos14.2 (arm64)
--
-- Host: localhost    Database: metaforge
-- ------------------------------------------------------
-- Server version	8.3.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Runtime configuration storage (2026.0.0).
CREATE TABLE IF NOT EXISTS `metaforge_configure` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `code` varchar(64) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `category` varchar(32) NOT NULL,
  `configure` text,
  `description` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configure_category_name` (`category`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `metaforge_chat`
--

DROP TABLE IF EXISTS `metaforge_chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `avatar` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `code` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_chat`
--

LOCK TABLES `metaforge_chat` WRITE;
/*!40000 ALTER TABLE `metaforge_chat` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_chat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_chat_user_relation`
--

DROP TABLE IF EXISTS `metaforge_chat_user_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_chat_user_relation` (
  `chat_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_chat_user_relation`
--

LOCK TABLES `metaforge_chat_user_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_chat_user_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_chat_user_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dashboard`
--

DROP TABLE IF EXISTS `metaforge_dashboard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dashboard` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `configure` longtext,
  `description` varchar(1000) DEFAULT NULL COMMENT 'Description',
  `code` varchar(100) DEFAULT NULL,
  `avatar` text,
  `version` varchar(100) DEFAULT '2.0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dashboard`
--

LOCK TABLES `metaforge_dashboard` WRITE;
/*!40000 ALTER TABLE `metaforge_dashboard` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dashboard` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dashboard_report_relation`
--

DROP TABLE IF EXISTS `metaforge_dashboard_report_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dashboard_report_relation` (
  `dashboard_id` bigint DEFAULT NULL,
  `report_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dashboard_report_relation`
--

LOCK TABLES `metaforge_dashboard_report_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_dashboard_report_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dashboard_report_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dashboard_user_relation`
--

DROP TABLE IF EXISTS `metaforge_dashboard_user_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dashboard_user_relation` (
  `dashboard_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dashboard_user_relation`
--

LOCK TABLES `metaforge_dashboard_user_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_dashboard_user_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dashboard_user_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dataset`
--

DROP TABLE IF EXISTS `metaforge_dataset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dataset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `description` text,
  `query` longtext,
  `sync_mode` varchar(100) DEFAULT NULL,
  `expression` varchar(100) DEFAULT NULL,
  `state` varchar(100) DEFAULT NULL,
  `message` longtext,
  `table_name` varchar(255) DEFAULT NULL,
  `code` varchar(100) DEFAULT (uuid()),
  `column_mode` varchar(100) DEFAULT 'DIMENSION',
  `scheduler` varchar(100) DEFAULT 'LocalScheduler',
  `executor` varchar(100) DEFAULT 'Local',
  `total_rows` bigint DEFAULT '0',
  `total_size` varchar(100) DEFAULT NULL,
  `life_cycle` bigint DEFAULT '0',
  `life_cycle_column` varchar(100) DEFAULT NULL,
  `life_cycle_type` varchar(100) DEFAULT NULL,
  `is_custom_column` tinyint(1) DEFAULT '0',
  `is_virtual_column` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dataset`
--

LOCK TABLES `metaforge_dataset` WRITE;
/*!40000 ALTER TABLE `metaforge_dataset` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dataset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dataset_column`
--

DROP TABLE IF EXISTS `metaforge_dataset_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dataset_column` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `description` text,
  `type` varchar(100) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `original` varchar(255) DEFAULT NULL,
  `default_value` varchar(255) DEFAULT NULL,
  `position` int DEFAULT NULL,
  `is_nullable` tinyint(1) DEFAULT '0',
  `length` int DEFAULT NULL,
  `dataset_id` int DEFAULT NULL,
  `is_order_by_key` tinyint(1) DEFAULT '0',
  `column_mode` varchar(100) DEFAULT 'DIMENSION',
  `is_partition_key` tinyint(1) DEFAULT '0',
  `alias_name` varchar(200) DEFAULT NULL,
  `is_primary_key` varchar(100) DEFAULT '0',
  `is_sampling_key` tinyint(1) DEFAULT '0',
  `is_custom_column` tinyint(1) DEFAULT '0',
  `is_virtual_column` tinyint(1) DEFAULT '0',
  `code` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dataset_column`
--

LOCK TABLES `metaforge_dataset_column` WRITE;
/*!40000 ALTER TABLE `metaforge_dataset_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dataset_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dataset_column_relation`
--

DROP TABLE IF EXISTS `metaforge_dataset_column_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dataset_column_relation` (
  `dataset_id` bigint DEFAULT NULL,
  `column_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dataset_column_relation`
--

LOCK TABLES `metaforge_dataset_column_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_dataset_column_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dataset_column_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dataset_history`
--

DROP TABLE IF EXISTS `metaforge_dataset_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dataset_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `query` varchar(255) DEFAULT NULL,
  `message` text,
  `elapsed` bigint DEFAULT NULL,
  `count` int DEFAULT NULL,
  `total_count` bigint DEFAULT NULL,
  `processed_count` bigint DEFAULT NULL,
  `progress` decimal(5,2) DEFAULT NULL,
  `task_name` varchar(64) DEFAULT NULL,
  `work_home` varchar(512) DEFAULT NULL,
  `executor_configure` text,
  `query_mode` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `code` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dataset_history`
--

LOCK TABLES `metaforge_dataset_history` WRITE;
/*!40000 ALTER TABLE `metaforge_dataset_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dataset_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dataset_history_relation`
--

DROP TABLE IF EXISTS `metaforge_dataset_history_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dataset_history_relation` (
  `dataset_history_id` bigint DEFAULT NULL,
  `dataset_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dataset_history_relation`
--

LOCK TABLES `metaforge_dataset_history_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_dataset_history_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dataset_history_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dataset_source_relation`
--

DROP TABLE IF EXISTS `metaforge_dataset_source_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dataset_source_relation` (
  `dataset_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dataset_source_relation`
--

LOCK TABLES `metaforge_dataset_source_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_dataset_source_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dataset_source_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_dataset_user_relation`
--

DROP TABLE IF EXISTS `metaforge_dataset_user_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_dataset_user_relation` (
  `dataset_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_dataset_user_relation`
--

LOCK TABLES `metaforge_dataset_user_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_dataset_user_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_dataset_user_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_function`
--

DROP TABLE IF EXISTS `metaforge_function`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_function` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT 'Function name',
  `content` varchar(255) DEFAULT NULL COMMENT 'Expression of function',
  `description` text COMMENT 'Function description',
  `plugin` varchar(255) DEFAULT NULL COMMENT 'Trial plug-in, multiple according to, split',
  `example` text COMMENT 'Function Usage Example',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(20) DEFAULT 'KEYWORDS',
  `active` tinyint(1) DEFAULT '1',
  `code` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Plug-in correlation function';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_function`
--

LOCK TABLES `metaforge_function` WRITE;
/*!40000 ALTER TABLE `metaforge_function` DISABLE KEYS */;
INSERT INTO `metaforge_function` VALUES (1,'SHOW','SHOW','SHOW 修改','Redis,Zookeeper,Alioss,Kafka,H2,Hdfs,MySQL',NULL,'2024-11-07 21:33:27','2024-11-25 21:43:52','KEYWORD',1,'8daa7c0812b24c2fa295d0b19e0b46a9'),(2,'SELECT','SELECT','SELECT','H2,MySQL,Hdfs,ClickHouse,Presto,Redis',NULL,'2024-11-07 22:11:19','2024-11-07 22:11:19','KEYWORD',1,'1de4c62f66c84f79a1b1687ef727fabd');
/*!40000 ALTER TABLE `metaforge_function` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_menu`
--

DROP TABLE IF EXISTS `metaforge_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `url` varchar(255) NOT NULL,
  `group_name` varchar(255) DEFAULT NULL,
  `sorted` int DEFAULT '0',
  `type` varchar(10) DEFAULT 'VIEW',
  `parent` bigint DEFAULT '0',
  `active` tinyint(1) DEFAULT '1',
  `i18n_key` varchar(255) DEFAULT NULL,
  `icon` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `redirect` bigint DEFAULT '0',
  `is_new` tinyint(1) DEFAULT '0',
  `view` varchar(550) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17221 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_menu`
--

LOCK TABLES `metaforge_menu` WRITE;
/*!40000 ALTER TABLE `metaforge_menu` DISABLE KEYS */;
INSERT INTO `metaforge_menu` VALUES (1,'全局 - 首页','HOME','全局路由：所有用户都可以访问','/home',NULL,1,'VIEW',0,1,'common.home','Home','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(2,'全局 - 查询','QUERY','全局路由：所有用户都可以访问','/admin/query',NULL,3,'VIEW',0,1,'common.query','SquareChevronRight','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(3,'全局 - 管理主菜单','MANAGEMENT','全局：所有用户都可以访问\n位置：顶部管理主菜单','/admin',NULL,3,'VIEW',0,1,'common.admin','Hammer','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(4,'全局 - 管理 - 数据源','DATASOURCE','全局：所有用户都可以访问\n位置：顶部管理一级子菜单','/admin/source','default',1,'VIEW',3,1,'common.source','List','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(5,'全局 - 管理 - 片段','SNIPPET','全局：所有用户都可以访问\n位置：顶部管理一级子菜单','/admin/snippet',NULL,2,'VIEW',3,1,'common.snippet','SquareDashedBottomCode','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(6,'全局 - 管理 - 查询历史','HISTORY','全局：所有用户都可以访问\n位置：顶部管理一级子菜单','/admin/history',NULL,3,'VIEW',3,1,'common.history','History','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(8,'管理员 - 系统主菜单','SYSTEM','管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单','/system',NULL,4,'VIEW',0,1,'common.system','ShieldPlus','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(9,'管理员 - 系统 - 函数','FUNCTION','管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单','/system/function',NULL,6,'VIEW',3,1,'common.function','SquareFunction','2023-07-04 21:47:24','2024-11-25 21:37:49',0,0,NULL),(10,'管理员 - 系统 - 定时任务','SCHEDULE','管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单','/system/schedule',NULL,2,'VIEW',8,1,'common.schedule','Timer','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(12,'管理员 - 系统 - 权限','ROLE','管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单','/system/role',NULL,4,'VIEW',8,1,'common.authority','Flag','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(13,'管理员 - 系统 - 菜单','MENU','管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单','/system/menu',NULL,5,'VIEW',8,1,'common.menu','Menu','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,NULL),(14,'管理员 - 系统 - 用户','USERS','管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单','/system/user',NULL,6,'VIEW',8,1,'common.user','User','2023-07-04 21:47:24','2023-07-04 21:47:24',0,0,'@/views/pages/system/user/SystemUser.vue'),(15,'全局 - 管理 - 报表','REPORT','全局：所有用户都可以访问\n位置：顶部管理一级子菜单','/admin/report',NULL,6,'VIEW',3,1,'common.report','SquareKanban','2023-12-18 13:37:35',NULL,0,0,NULL),(16,'全局 - 仪表盘','DASHBOARD','全局路由：所有用户都可以访问','/admin/dashboard',NULL,2,'VIEW',0,1,'common.dashboard','Gauge','2023-12-19 10:26:21',NULL,0,0,NULL),(17,'全局 - 数据集','DATASET','全局路由：所有用户都可以访问','/admin/dataset',NULL,3,'VIEW',0,1,'common.dataset','Contrast','2023-12-21 11:32:33','2024-04-05 12:14:02',0,1,NULL),(18,'全局 - 商店','STORE','','/store','',3,'VIEW',0,1,'common.store','Store',NULL,'2024-11-05 21:18:28',0,0,NULL),(20,'测试菜单','10ae626195704b96b353d00aeb6ab1e5','我被修改了，修改了数据','/test','',2,'VIEW',0,1,'common.home','',NULL,'2024-11-25 21:28:13',0,0,NULL),(21,'管理员 - 管理 - 工作流','b6669c359a7d41e581d906446366daf3','','/admin/workflow','default',1,'VIEW',3,1,'common.workflow','Workflow','2024-12-19 18:36:22','2024-12-19 18:36:22',0,1,NULL);
/*!40000 ALTER TABLE `metaforge_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_message`
--

DROP TABLE IF EXISTS `metaforge_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `content` text,
  `model` varchar(255) DEFAULT NULL,
  `type` varchar(100) DEFAULT NULL,
  `prompt_tokens` bigint DEFAULT '0',
  `completion_tokens` bigint DEFAULT '0',
  `total_tokens` bigint DEFAULT '0',
  `code` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_message`
--

LOCK TABLES `metaforge_message` WRITE;
/*!40000 ALTER TABLE `metaforge_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_message_chat_relation`
--

DROP TABLE IF EXISTS `metaforge_message_chat_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_message_chat_relation` (
  `message_id` bigint DEFAULT NULL,
  `chat_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_message_chat_relation`
--

LOCK TABLES `metaforge_message_chat_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_message_chat_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_message_chat_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_message_user_relation`
--

DROP TABLE IF EXISTS `metaforge_message_user_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_message_user_relation` (
  `message_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_message_user_relation`
--

LOCK TABLES `metaforge_message_user_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_message_user_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_message_user_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_notification`
--

DROP TABLE IF EXISTS `metaforge_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `content` varchar(255) NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `is_read` bit(1) NOT NULL DEFAULT b'0',
  `user_id` bigint DEFAULT NULL,
  `entity_type` varchar(255) DEFAULT NULL,
  `entity_code` varchar(255) DEFAULT NULL,
  `entity_name` varchar(255) DEFAULT NULL,
  `entity_exists` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_notification`
--

LOCK TABLES `metaforge_notification` WRITE;
/*!40000 ALTER TABLE `metaforge_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_report`
--

DROP TABLE IF EXISTS `metaforge_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `configure` longtext,
  `type` varchar(255) DEFAULT NULL,
  `realtime` tinyint(1) DEFAULT '0',
  `query` longtext,
  `code` varchar(100) DEFAULT NULL,
  `description` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_report`
--

LOCK TABLES `metaforge_report` WRITE;
/*!40000 ALTER TABLE `metaforge_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_report_dataset_relation`
--

DROP TABLE IF EXISTS `metaforge_report_dataset_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_report_dataset_relation` (
  `report_id` bigint DEFAULT NULL,
  `dataset_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_report_dataset_relation`
--

LOCK TABLES `metaforge_report_dataset_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_report_dataset_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_report_dataset_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_report_source_relation`
--

DROP TABLE IF EXISTS `metaforge_report_source_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_report_source_relation` (
  `report_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_report_source_relation`
--

LOCK TABLES `metaforge_report_source_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_report_source_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_report_source_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_report_user_relation`
--

DROP TABLE IF EXISTS `metaforge_report_user_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_report_user_relation` (
  `report_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_report_user_relation`
--

LOCK TABLES `metaforge_report_user_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_report_user_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_report_user_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_role`
--

DROP TABLE IF EXISTS `metaforge_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT ' ',
  `description` varchar(255) DEFAULT NULL COMMENT ' ',
  `create_time` datetime(5) DEFAULT CURRENT_TIMESTAMP(5),
  `active` tinyint(1) DEFAULT '1',
  `code` varchar(100) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_role`
--

LOCK TABLES `metaforge_role` WRITE;
/*!40000 ALTER TABLE `metaforge_role` DISABLE KEYS */;
INSERT INTO `metaforge_role` VALUES (1,'系统管理员','这是管理员路由，可以管理站点所有功能',NULL,1,'ADMIN','2025-02-08 09:26:54'),(2,'普通用户','我是一个普通用户的路由',NULL,1,'USER','2024-11-25 21:32:56'),(3,'Test001','这是一个测试路由，没有任何权限',NULL,1,'ROLE_TEST001',NULL),(4,'测试路由','用于测试功能，修改数据','2024-11-25 20:52:48.00000',1,'cc4','2024-11-26 13:50:55');
/*!40000 ALTER TABLE `metaforge_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_role_menu_relation`
--

DROP TABLE IF EXISTS `metaforge_role_menu_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_role_menu_relation` (
  `role_id` mediumtext,
  `menu_id` mediumtext
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_role_menu_relation`
--

LOCK TABLES `metaforge_role_menu_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_role_menu_relation` DISABLE KEYS */;
INSERT INTO `metaforge_role_menu_relation` VALUES ('2','6'),('2','16'),('2','5'),('2','1'),('2','3'),('2','7'),('2','2'),('2','17'),('2','4'),('2','15'),('4','20'),('1','10'),('1','6'),('1','4'),('1','12'),('1','15'),('1','14'),('1','5'),('1','3'),('1','18'),('1','13'),('1','17'),('1','9'),('1','1'),('1','8'),('1','16'),('1','2'),('1','21'),('1','7');
/*!40000 ALTER TABLE `metaforge_role_menu_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_scheduled`
--

DROP TABLE IF EXISTS `metaforge_scheduled`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_scheduled` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `expression` varchar(100) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `is_system` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_scheduled`
--

LOCK TABLES `metaforge_scheduled` WRITE;
/*!40000 ALTER TABLE `metaforge_scheduled` DISABLE KEYS */;
INSERT INTO `metaforge_scheduled` VALUES (1,'Synchronize table structure','Synchronize the table structure of the data source library at 1 am every day','0 0 * * * ?',1,1,'2023-07-04 21:47:24','2023-10-10 11:59:29','SOURCE_SYNCHRONIZE',NULL),(2,'Check source available','Check the availability of the data source every 1 hour','0 0 * * * ?',1,1,'2023-08-08 11:54:01','2023-08-09 18:25:26','SOURCE_CHECK',NULL);
/*!40000 ALTER TABLE `metaforge_scheduled` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_scheduled_history`
--

DROP TABLE IF EXISTS `metaforge_scheduled_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_scheduled_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `info` text,
  `state` varchar(100) DEFAULT NULL,
  `message` text,
  `code` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_scheduled_history`
--

LOCK TABLES `metaforge_scheduled_history` WRITE;
/*!40000 ALTER TABLE `metaforge_scheduled_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_scheduled_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_scheduled_history_relation`
--

DROP TABLE IF EXISTS `metaforge_scheduled_history_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_scheduled_history_relation` (
  `scheduled_id` int DEFAULT NULL,
  `history_id` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_scheduled_history_relation`
--

LOCK TABLES `metaforge_scheduled_history_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_scheduled_history_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_scheduled_history_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_scheduled_history_source_relation`
--

DROP TABLE IF EXISTS `metaforge_scheduled_history_source_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_scheduled_history_source_relation` (
  `scheduled_history_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_scheduled_history_source_relation`
--

LOCK TABLES `metaforge_scheduled_history_source_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_scheduled_history_source_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_scheduled_history_source_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_snippet`
--

DROP TABLE IF EXISTS `metaforge_snippet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_snippet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT ' ',
  `description` varchar(255) DEFAULT NULL COMMENT ' ',
  `context` text,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` bigint NOT NULL,
  `code` varchar(100) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  FULLTEXT KEY `full_text_index_for_code` (`context`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_snippet`
--

LOCK TABLES `metaforge_snippet` WRITE;
/*!40000 ALTER TABLE `metaforge_snippet` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_snippet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_source`
--

DROP TABLE IF EXISTS `metaforge_source`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_source` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `_catalog` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `_database` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `host` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `port` bigint NOT NULL,
  `protocol` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `_type` varchar(100) NOT NULL,
  `ssl` tinyint(1) DEFAULT '0',
  `_ssl` tinyint(1) DEFAULT '0',
  `publish` tinyint(1) DEFAULT '0',
  `public` tinyint(1) DEFAULT '0',
  `user_id` bigint DEFAULT NULL,
  `configure` text,
  `used_config` tinyint(1) DEFAULT '0',
  `version` varchar(255) DEFAULT NULL,
  `available` tinyint(1) DEFAULT '1',
  `message` text,
  `update_time` datetime DEFAULT NULL,
  `code` varchar(100) DEFAULT (replace(uuid(),_utf8mb3'-',_utf8mb4'')),
  `active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='The storage is used to query the data connection source';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_source`
--

LOCK TABLES `metaforge_source` WRITE;
/*!40000 ALTER TABLE `metaforge_source` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_source` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_source_query`
--

DROP TABLE IF EXISTS `metaforge_source_query`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_source_query` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `state` varchar(255) DEFAULT NULL,
  `create_time` mediumtext,
  `update_time` datetime DEFAULT NULL,
  `plugin_id` bigint NOT NULL,
  `content` text,
  `message` text,
  `elapsed` bigint DEFAULT '0',
  `user_id` bigint NOT NULL,
  `count` bigint DEFAULT '0',
  `query_mode` varchar(100) DEFAULT 'ADHOC',
  `active` tinyint(1) DEFAULT '1',
  `code` varchar(100) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `home` varchar(500) DEFAULT NULL,
  `format` varchar(100) DEFAULT 'Json',
  PRIMARY KEY (`id`),
  FULLTEXT KEY `full_text_index_for_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_source_query`
--

LOCK TABLES `metaforge_source_query` WRITE;
/*!40000 ALTER TABLE `metaforge_source_query` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_source_query` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_user`
--

DROP TABLE IF EXISTS `metaforge_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL COMMENT ' ',
  `password` varchar(255) DEFAULT NULL COMMENT ' ',
  `create_time` datetime(5) DEFAULT CURRENT_TIMESTAMP(5),
  `chat_configure` text,
  `is_system` tinyint(1) DEFAULT '0',
  `editor_configure` text,
  `avatar_configure` longtext COMMENT 'avatar configure',
  `active` tinyint(1) DEFAULT '1',
  `code` varchar(200) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `notify_configure` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_user`
--

LOCK TABLES `metaforge_user` WRITE;
/*!40000 ALTER TABLE `metaforge_user` DISABLE KEYS */;
INSERT INTO `metaforge_user` VALUES (1,'admin','$2a$10$FyWYvR61FHzT1szZtV69j.APDCrAqRcqMO.CiUYOSRiXmvugDsALu','2023-07-04 21:47:24.57480','{\"type\":\"ChatGPT\",\"host\":null,\"token\":null,\"timeout\":0,\"contentCount\":5}',0,'{\"fontSize\":12,\"theme\":\"chrome\"}','{\"type\":\"LocalFs\",\"path\":\"http://localhost:9096/upload/admin/avatar/avatar.png\",\"local\":\"/Users/shicheng/Code/metaforge/data/admin/avatar/avatar.png\"}',1,'admin',NULL,'2025-03-10 16:19:58','[{\"enabled\":true,\"type\":\"Internal\",\"services\":[\"CREATED\",\"SYNCDATA\",\"UPDATED\"]},{\"enabled\":false,\"type\":\"DingTalk\",\"services\":[]}]'),(2,'metaforge','$2a$10$bZ4XBRlYUjKfkBovWT9TuuXlEF7lpRxVrXS8iqyCjCHUqy4RPTL8.','2023-07-04 21:47:24.57542','{\"type\":\"ChatGPT\",\"host\":\"\",\"token\":null,\"timeout\":60,\"contentCount\":10}',0,'{\"fontSize\":12,\"theme\":\"chrome\"}','{\"path\":\"data:image/jpeg;base64,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/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFea/8LNf/AKBQ/wC//wD9jR/ws1/+gUP+/wD/APY0AelUV5r/AMLNf/oFD/v/AP8A2NH/AAs1/wDoFD/v/wD/AGNAHpVFZfhnVTrWjw3xh8nzCw2bt2MMR1wPStSgAooooAzvD3/IIg+rf+hGtEVneHv+QRB9W/8AQjWiKACuH+Lf/IBtP+vkf+gtXcVw/wAW/wDkA2n/AF8j/wBAagDyg9aKDSUALRWx/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAGNRWz/wAIzrf/AEC7r/vg0f8ACM63/wBAu6/74NAG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XNS+PUD+FdQz1CqR9dwoA30dXRXQgqwyCO4pawfA88k3h2GOY5lt2e3b/gJI/lit6gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKiu7mKztZbi4cJFGpZmPYVLXH/E2eQaLFawnDXDnPuqqWP8AKgDprS+hutNjvo9ywunmDcMED3rF8Fwm5t59YuMm5vpGYE/wxg4VR+VWoYifByRxcE2IC/Xy6TwU6v4W00p0EQH40AT6tq6WM0VtDBLd3so3JBF12/3iTwB71UXxDJb3EcWr6fLYLIQqSs4ePJ6AsOlZuq2usQ63qEmnWoke8jSOO5EgHkqM54NU/FWhLpnh+W5tbidmQDz1llLrMDweD354PtQAujaNrF813Jcutks8rmSUoHmkGcAAn7q46Cs3VfDZ8PajbSW1yjWE/wC7Zr2MSRo/JAYcAA+vau58K3D3Ph3TppfvtCufyrH1xH8U3U2k2r7LG3/4+Jxzuk6qg+h5NAGVZWGp+LJFOo3EcejwN+7+yqUWUjjK55wOefyrtLS1sdIstsKRW1vGMk/dA9yapeE7s3WiRJIAs9uTBKo7Mpx/LBqlNENf8Q3FrcZOn6eVDRfwyyMM8+uKALf/AAlej7youwwHVgjFR+OMVs288VzCstvIksbdGQ5BrO1HUtO0mOGCchBLwkSRliR9AKw7uK50DXoodEhhaHUQx+zuxVEdeSwx0yD0oA1fEGjfawt5YFYdTg+aKQD73+y3qDXO+IbWXWvA9o+jReWFfe9tH+IYY9Qea3d/igDJh0c/7IeT+dYnh/WZNLk1SO+sZVh+2MzyW48xImbBIPTj396AMzQLDXddv9Ob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w/yrP8Lf8i1pX/XrH/6CKANSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqveWNpe7PtlrBcbDlfNjD7fpmrFFADY0WNAsahVHAAGAKdRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAY/ir/kFL/wBfNv8A+jkq5rP/ACB77/rg/wD6Cap+Kv8AkFL/ANfNv/6OSrms/wDIHvv+uD/+gmgD57Wl70i0vemI9p+HH/In2X+9J/6G1dNXM/Dj/kT7L/ek/wDQ2rpqQwooooAzvD3/ACCIPq3/AKEa0RWd4e/5BEH1b/0I1oigArh/i1/yAbT/AK+R/wCgNXcVw/xa/wCQDaf9fI/9AagDyc/eFTWf/H1D/vj+dQn7wqaz/wCPqH/fH86APoa5/wCPaX/cP8qz/C3/ACLWlf8AXrH/AOgitC5/49pf9w/yrP8AC3/ItaV/16x/+gigDUooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAx/FX/ACCl/wCvm3/9HJVzWf8AkD33/XB//QTVPxV/yCl/6+bf/wBHJVzWf+QPff8AXB//AEE0AfPa0vekWl70xHtPw4/5E+y/3pP/AENq6auZ+HH/ACJ9l/vSf+htXTUhhRRRQBneHv8AkEQfVv8A0I1ois7w9/yCIPq3/oRrRFABXD/Fr/kA2n/XyP8A0Bq7iuH+LX/IBtP+vkf+gNQB5OfvCprP/j6h/wB8fzqE/eFTWf8Ax9Q/74/nQB9DXP8Ax7S/7h/lWf4W/wCRa0r/AK9Y/wD0EVoXP/HtL/uH+VZ/hb/kWtK/69Y//QRQBqUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAY/ir/AJBS/wDXzb/+jkq5rP8AyB77/rg//oJqn4q/5BS/9fNv/wCjkq5rP/IHvv8Arg//AKCaAPntaXvSLS96Yj2n4cf8ifZf70n/AKG1dNXM/Dj/AJE+y/3pP/Q2rpqQwooooAzvD3/IIg+rf+hGtEVneHv+QRB9W/8AQjWiKACuH+LX/IBtP+vkf+gNXcVw/wAWv+QDaf8AXyP/AEBqAPJz94VNZ/8AH1D/AL4/nUJ+8Kms/wDj6h/3x/OgD6Guf+PaX/cP8qz/AAt/yLWlf9esf/oIrQuf+PaX/cP8qz/C3/ItaV/16x/+gigDUooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKpavqlno9mbrUp1gtwQpdgTyenSgC7RXDeFfG9jealeafdalDPK12VsnC7fNjIBAxjgjkc+lP0vW/FOrxT3Gn2WkC2S4lhXzZJAx2OVycD2oA7aiuX0DWdXm8R3Wk6zb2UbxWy3CtbOxyGYjB3fStzVbi7trQyafZC9n3AeV5oj47nJoAuUV55pni7xAbfW7240MT2lpcSLxdxr5QRRuXp82DnmtW68R6nNe6Xa6Np9tLLe2X25vtNwUCLlRt4U5PzUAddRXN+Hta1K71q/0zV7K2tp7aKOUGCYyKwct3Kj+7+tb17dQ2VpNdXUixwQoZHduiqBkmgCaiuKtfEt1B4Wl1rUF3yX0pOmWagbircRJ7k/eJ7A+1LpPiHXobG2g1Lw7qE98sY811khUM3cgBulAHaUVheH/ABA+rX99Zzabc2M9oqM6zMhzvzj7pPpVvX9YstFsJbi+uIogEZkV3CmQqM4XPU0AaVFc74K1qPUdDsFudStLvVDArzrFIhYE8n5VPGM4qloniy0n1TXBf6pZRW0N15FtHJKiEBVAY+py2aAOvoqpp+pWWohzYXltchDhjDKHx9cVX1251S2iibR9OgvnJO9ZbnyAo9QdrZoA06K4aw8UeJb67vre38NWRkspRFLu1LAyVB4/d88EUs/ibxLDqtpp7+G7Hz7lHePGpHGFxnJ8rjqKAO4oqnpM17PZq+p2kVpcknMUc3mgDsd2B/KsvxBq9/ot3HdyWq3GibcTtECZbc5++R/EvrjkUAdBRXHy65rd5r2o2ejR6Z9ms44XMt0Xy3mAkdPpVNde8UPrc+mougF4YVmeQvJtG4kBevXgmgDvKKztDfU5Ld21f7CZC3yG0LFSvvu71ja14yGk3620+i6tIHlEMUkcSlZXPQKd3PegDqqK5qbUPEN5BBPp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P8Lf8i1pX/XrH/6CKANSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACvIv2j/8AkXdL/wCvo/8AoJr12vIv2j/+Rd0v/r6P/oJoA9J8K/8AIsaT/wBesX/oIrUrL8K/8ixpP/XrF/6CK1KACiiigAoozRQAUUUUAFcl4t8b2fh3W9L0mS3mnvL90VNpAVVZ9uSfr2xXW14j8YP+Sq+EvrB/6PNAHt1FFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAGP4q/5BS/9fNv/wCjkq5rP/IHvv8Arg//AKCap+Kv+QUv/Xzb/wDo5Kuaz/yB77/rg/8A6CaAPntaXvSLS96Yj2n4cf8AIn2X+9J/6G1dNXM/Dj/kT7L/AHpP/Q2rpqQwooooAzvD3/IIg+rf+hGtEVneHv8AkEQfVv8A0I1oigAriPi1/wAgK0/6+R/6A1dvXEfFr/kBWn/XyP8A0BqAPJj94VNZ/wDH1D/vj+dQn7wqaz/4+of98fzoA+hrn/j2l/3D/Ks/wt/yLWlf9esf/oIrQuf+PaX/AHD/ACrP8Lf8i1pX/XrH/wCgigDUooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKyPEniPTPDlibrVrlYUPCL1Zz6KO9S+I9YttA0W61K9P7mBdxA6segA9ycCvGPB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CiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAK8T8M/wDJwer/AO7L/wCgivbK8+0jwRfWXxOvvE0lxbNaThgsSlt4yAOeMdqAPQaKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAMfxV/yCl/6+bf/wBHJVzWf+QPff8AXB//AEE1T8Vf8gpf+vm3/wDRyVc1n/kD33/XB/8A0E0AfPa0vekWl70xHtPw4/5E+y/3pP8A0Nq6auZ+HH/In2X+9J/6G1dNSGFFFFAGd4e/5BEH1b/0I1ois7w9/wAgiD6t/wChGtEUAFcR8Wv+QFaf9fI/9Aau3riPi1/yArT/AK+R/wCgNQB5MfvCprP/AI+of98fzqE/eFTWf/H1D/vj+dAH0Nc/8e0v+4f5Vn+Fv+Ra0r/r1j/9BFaFz/x7S/7h/lWf4W/5FrSv+vWP/wBBFAGpRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUU13VAN7BckAZOMn0oAdRRRQAUUUUAFFFFABRRRQAUUUUAFFFUdT1FLF7OMo0kt1OIUUHHYkk+wAJoAvUUVR1nUk0yzEzo0rPIkUca9XdmCgfmaAL1FFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRVKDUEn1W6sY0Ym2RGkk7BmyQv1wAfxFWvMX+8KAH0UzzF/vL+dPoAKKKQOpdlDAsuMjPIzQAtFFFABRRRQAUUUUAFFFQi6ty5QTxFx/DvGaAJqKKKACiqOsaiumwQuY2lkmmSCONTyzMcfoMk+wNXqACiikR1dcowYZIyDmgBa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+R/6A1dvXEfFr/kBWn/XyP8A0BqAPJj94VNZ/wDH1D/vj+dQn7wqaz/4+of98fzoA+hrn/j2l/3D/Ks/wt/yLWlf9esf/oIrQuf+PaX/AHD/ACrP8Lf8i1pX/XrH/wCgigDUooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAx/FX/IKX/r5t/8A0clXNZ/5A99/1wf/ANBNU/FX/IKX/r5t/wD0clXNZ/5A99/1wf8A9BNAHz2tL3pFpe9MR7T8OP8AkT7L/ek/9Daumrmfhx/yJ9l/vSf+htXTUhhRRRQBneHv+QRB9W/9CNaIrO8Pf8giD6t/6Ea0RQAVxHxa/wCQFaf9fI/9Aau3riPi1/yArT/r5H/oDUAeTH7wqaz/AOPqH/fH86hP3hU1n/x9Q/74/nQB9DXP/HtL/uH+VZ/hb/kWtK/69Y//AEEVoXP/AB7S/wC4f5Vn+Fv+Ra0r/r1j/wDQRQBqUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRWPoWuLqc11a3Fu9nqNq2Jrd23YB+6yt/Ep9aANiiiq2p3TWVhNcpbTXTRLu8mEAu30B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w/yrP8Lf8i1pX/XrH/6CK0Ln/j2l/wBw/wAqz/C3/ItaV/16x/8AoIoA1KKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAMfxX/yCl/6+bf/ANHJVzWf+QPff9cH/wDQTVPxX/yCl/6+bf8A9HJVzWf+QPff9cH/APQTQB89rS96RaXvTEe0/Dj/AJE+y/3pP/Q2rpq5n4cf8ifZf70n/obV01IYUUUUAZ3h7/kEQfVv/QjWiKzvD3/IIg+rf+hGtEUAFcR8Wv8AkBWn/XyP/QGrt64j4tf8gK0/6+R/6A1AHkx+8Kms/wDj6h/3x/OoT94VNZ/8fUP++P50AfQ1z/x7S/7h/lWf4W/5FrSv+vWP/wBBFaFz/wAe0v8AuH+VZ/hb/kWtK/69Y/8A0EUAalFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAGP4r/5BS/9fNv/AOjkq5rP/IHvv+uD/wDoJqn4r/5BS/8AXzb/APo5Kuaz/wAge+/64P8A+gmgD57Wl70i0vemI9p+HH/In2X+9J/6G1dNXM/Dj/kT7L/ek/8AQ2rpqQwooooAzvD3/IIg+rf+hGtEVneHv+QRB9W/9CNaIoAK4j4tf8gK0/6+R/6A1dvXEfFr/kBWn/XyP/QGoA8mP3hU1n/x9Q/74/nUJ+8Kms/+PqH/AHx/OgD6Guf+PaX/AHD/ACrP8Lf8i1pX/XrH/wCgitC5/wCPaX/cP8qz/C3/ACLWlf8AXrH/AOgigDUooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiigmgAorF8U+JdL8MacbzV7pYYzkIvV5D6KO5rx7WvjBrl9IRoOn29hbnhZbr947f8BHA/WonOMFdnNXxlLD/xHqe90V8zyeP/AByXDDXowP7gsosfyzW9ofxi1qxmRPEVhDe2ucNPajZIo9dvQ/his1iabdrnJDN8PJ21R71RWZ4f1rT/ABBpsd/pNwlxbyd16qfQjsfatOtz04yUldBRRRQMKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoqrqV5bafZy3d7MkFvENzyOcBRXini744FZHt/DNopA4+1XIOD9F4/X8qipUjTV5HZhMBXxkrUY38+h7rRXyjL8XPGbSErqqRj+6trCQPzU1r6L8a/EVpKP7SS31CLuCoib81GP0rBYym2erPhnGRV1Z+j/zSPpeiuS8D+OtJ8XwH7C7RXaDMltJ99ff0I9xXW10RkpK6PCq0p0ZOFRWaCiiiqMwooooAKK8U/wCGjfCX/CbDQBBemHz/ALN9v2r5W/dtzjO7bnvXtdABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQBj+K/wDkFL/182//AKOSrms/8ge+/wCuD/8AoJqn4r/5BS/9fNv/AOjkq5rP/IHvv+uD/wDoJoA+e1pe9ItL3piPafhx/wAifZf70n/obV01cz8OP+RPsv8Aek/9DaumpDCiiigDO8Pf8giD6t/6Ea0RWd4e/wCQRB9W/wDQjWiKACuI+LX/ACArT/r5H/oDV29cR8Wv+QFaf9fI/wDQGoA8mP3hU1n/AMfUP++P51CfvCprP/j6h/3x/OgD6Guf+PaX/cP8qz/C3/ItaV/16x/+gitC5/49pf8AcP8AKs/wt/yLWlf9esf/AKCKANSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKpaxqNvpOlXV/ettt7aMyOfYVdry79oK9MPg62sVYgX12kT47oMsR+YFKTsmzKvU9lTlPsePazq974p1aXWdWzvfiCH+GBOwHv3J96r9B7U9umF4rovht4PTxrrV4L95E0jT9olRDgzOwJC5HIAxz9RXkrmrz1Pi4RqYyrbds5hbmPcUEilvQNzTyc9a+hr34ZeE7rTzaDR7aBcYWSFdsin1DDmvHJfhz4qt9an0qytBdQRNmK/lYJGyHoT6sO4Ga1nhJRWh118qrUbW1v2M7wf4kn8Ga7Hfwbjp07BL2AdCv8AfH+0K+obS4iureOeBw8Uih0YdCCMg14/oXwWRtsnifVJLkjn7Nafu4x9T1P6V61plhb6ZYW9lZR+XbQII41yThR0GTXZRjKMbSZ7eWUa9Gny1dugmtarY6JplxqOq3MdrZQLuklkOAB/U+1eGax+0/4ctrxotM0fUb+BTjzyyxA+4ByfzxXF/teeJbq+8U6d4VtpGFrbxLPIoPDzOSBn6LjH+8a9d+HvwU8J6H4btItU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oooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAMUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUYFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAVzvg/hNX/7CE3866Kue8Ifc1f8A7CE386ALPir/AJBK/wDXzb/+jUq5rP8AyB77/rg//oJqn4q/5BK/9fNv/wCjUq5rP/IHvv8Arg//AKCaAPntaXvSLS96Yj2n4cf8ifZf70n/AKG1dNXM/Dj/AJE+y/3pP/Q2rpqQwooooAzvD3/IIg+rf+hGtEVneHv+QRB9W/8AQjWiKACuI+LX/ICtP+vkf+gNXb1xHxa/5AVp/wBfI/8AQGoA8mP3hU1n/wAfUP8Avj+dQn7wqaz/AOPqH/fH86APoa5/49pf9w/yrP8AC3/ItaV/16x/+gitC5/49pf9w/yrP8Lf8i1pX/XrH/6CKAKet/8AI1eH/wDem/8AQK6Cuf1v/kavD/8AvTf+gV0FABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUCgAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKK5DxP4uk0nxn4X8P2tqk8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KKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKwPGLNLZ22nRsVa/nWFiOoTq36D9a364/4jSta29hdjIEbSLkdi0ZAP50AOgW915SthOdO0aL91G8a/vJQvGQewqpb2kv22403wywgSNsXuoSne7P/AHVz3rodPZbHwnA8IAEVmHH1CZqt4Eh8rw5bOeXnzM57szHJNAFJvBhYM7a3qZnPVzLx+VUF1PU/DGoR2evt9t02c7Y7nHK/X/CtrVfFUWn300H2O4mjtwpuJo8bYt3TNV/iObaTwu4lKszunk+pbPb8M0AQT2MHhnVf7WtYVbTrgBZlAyYc/wAS/wCz6iuujZZY1ZGDIwyrDuKyfC+b3wrYC7AkD24Rgf4hjHP4VQ0y4Ph69bTL5/8AQ3y9nM57DrGT6jtQBOMXnjU4+7YW2Poz/wD1hUSxjWvFU3n4a00vCrGejSsMkn1wP51P4QRpbW61OYYkv5TKAeyDhf0FR6IfsXijWbOUYa5ZbqI/3lI2nH0IoAu6xrRsbiK1trOa8vJBuEUZA2r/AHmJ6CnaTrCX88ltNby2l7GAzQy4zj1BHBFQappt7/a0epaVLAs/l+TKk4O1lznt3rM8RX0dl4q0uZYppJIonM4gjLkIcBcge9AGp4q0RNYsGCfu7uMExSLwQfQ+xpng/wCyyaHbyWltFbtgpIiryHHByfXimf8ACWWBHyxXzP8A3Psz5/lWH4fvtVmm1SHRrFUjkumk826O0REgZBUck+3vQB1Wt6rDpNp5kp3zP8sUQ+9I/YAVV8O2EttBPd6gwN9dN5kp/ujsg9hS6XoK2t0b6+na91BussnRPZB2FZ6RL4muri4vJHXSbaQxRxKxUSlfvO2O1AGpfaXpurN5isi3ScrPbuBIh9cj+tcxYW09prmk6NeInk27yXKTDpccHBPuCeay73WfDRmMWn281lMjEJeQYGCO5GeV+tdBdXL6x4Oi1VAovrRvOEij+JD82PYgGgDb8V/8gpf+viD/ANHJVzWf+QNff9e8n/oJrN8Qzrc6DBPH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A8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCEL17/AKB//kVP8a9looA8a/4QvXv+gf8A+RU/xo/4QvXv+gf/AORU/wAa9looA8a/4QvXv+gf/wCRU/xo/wCE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\",\"fsType\":\"Local\",\"local\":\"/Users/shicheng/Code/metaforge/data/metaforge/avatar///WechatIMG60.jpg\"}',1,'metaforge',NULL,NULL,NULL),(10000,'Ai',NULL,'2023-07-10 19:08:25.95841',NULL,0,'{}','{}',1,'ai',NULL,NULL,NULL),(10001,'captcha','$2a$10$eWebg/za0PcfvUd2En5YFuyXnJspHQhpit94FvedqGzUsU70/Lw9y','2023-08-29 14:45:14.14400',NULL,0,'{}','{}',1,'captcha',NULL,NULL,NULL),(10002,'aaa','$2a$10$JIEa.J/CcX5SUabH0/F7yuQEtP08X4VOmIWSo1uV9PoyAKNBmgS7m','2025-04-21 19:46:27.19000',NULL,0,'null','null',1,'2bb038bd6dca4084aadbc0468c9a2105',NULL,'2025-04-21 20:03:40','[]');
/*!40000 ALTER TABLE `metaforge_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_user_log`
--

DROP TABLE IF EXISTS `metaforge_user_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_user_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device` varchar(255) DEFAULT NULL COMMENT 'Login device',
  `client` varchar(255) DEFAULT NULL COMMENT 'Login client',
  `ip` varchar(100) DEFAULT NULL COMMENT 'Login ip',
  `message` varchar(225) DEFAULT NULL COMMENT 'Error message',
  `state` varchar(20) DEFAULT NULL COMMENT 'Login state',
  `ua` varchar(255) DEFAULT NULL COMMENT 'Trial plug-in, multiple according to, split',
  `user_id` bigint NOT NULL,
  `create_time` datetime(5) DEFAULT CURRENT_TIMESTAMP(5),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User login log';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_user_log`
--

LOCK TABLES `metaforge_user_log` WRITE;
/*!40000 ALTER TABLE `metaforge_user_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_user_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_user_role_relation`
--

DROP TABLE IF EXISTS `metaforge_user_role_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_user_role_relation` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_user_role_relation`
--

LOCK TABLES `metaforge_user_role_relation` WRITE;
/*!40000 ALTER TABLE `metaforge_user_role_relation` DISABLE KEYS */;
INSERT INTO `metaforge_user_role_relation` VALUES (2,2),(1,1),(10003,2),(10004,2),(10005,2),(10006,2),(10007,2),(10008,2),(10009,2),(10010,2),(10011,2),(10012,2),(10013,2),(10014,2),(10015,2),(10016,4);
/*!40000 ALTER TABLE `metaforge_user_role_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `metaforge_workflow`
--

DROP TABLE IF EXISTS `metaforge_workflow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metaforge_workflow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `work` varchar(255) DEFAULT NULL,
  `elapsed` bigint DEFAULT NULL,
  `executor` varchar(255) DEFAULT NULL,
  `configure` text,
  `j_from_id` bigint DEFAULT NULL,
  `j_to_id` bigint DEFAULT NULL,
  `j_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `metaforge_workflow`
--

LOCK TABLES `metaforge_workflow` WRITE;
/*!40000 ALTER TABLE `metaforge_workflow` DISABLE KEYS */;
/*!40000 ALTER TABLE `metaforge_workflow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oauth_identities`
--

DROP TABLE IF EXISTS `oauth_identities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth_identities` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `provider` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_user_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `access_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_data` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_identity` (`provider`,`provider_user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `oauth_identities_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oauth_identities`
--

LOCK TABLES `oauth_identities` WRITE;
/*!40000 ALTER TABLE `oauth_identities` DISABLE KEYS */;
/*!40000 ALTER TABLE `oauth_identities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
-- 新菜单：管理员 → 系统 → 运行时配置（/system/configure）
-- Admin menu entry for the runtime configuration page. Idempotent insert by `code`.
INSERT INTO `metaforge_menu` (`name`, `code`, `description`, `url`, `group_name`, `sorted`, `type`, `parent`, `active`, `i18n_key`, `icon`)
SELECT '管理员 - 系统 - 运行时配置', 'RUNTIME_CONFIGURE',
       '管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单',
       '/system/configure', NULL, 7, 'VIEW', 8, 1, 'configure.runtime.title', 'Settings2'
WHERE NOT EXISTS (SELECT 1 FROM `metaforge_menu` WHERE `code` = 'RUNTIME_CONFIGURE');

-- 绑定该菜单到 admin 角色（role_id=1）。menu_id 字段为 mediumtext，CAST 后插入。
INSERT INTO `metaforge_role_menu_relation` (`role_id`, `menu_id`)
SELECT '1', CAST(`id` AS CHAR)
FROM `metaforge_menu`
WHERE `code` = 'RUNTIME_CONFIGURE'
  AND NOT EXISTS (
    SELECT 1 FROM `metaforge_role_menu_relation` r
    WHERE r.role_id = '1' AND r.menu_id = CAST(`metaforge_menu`.id AS CHAR)
);

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-07-28 13:11:18
