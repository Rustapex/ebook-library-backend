-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: localhost    Database: abc
-- ------------------------------------------------------
-- Server version	8.0.40

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

--
-- Current Database: `abc`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `abc` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `abc`;

--
-- Table structure for table `admin_audit_log`
--

DROP TABLE IF EXISTS `admin_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_audit_log` (
  `audit_log_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `admin_member_id` bigint unsigned DEFAULT NULL,
  `action_type` varchar(50) NOT NULL,
  `target_type` varchar(50) NOT NULL,
  `target_id` bigint unsigned DEFAULT NULL,
  `before_summary` text,
  `after_summary` text,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`audit_log_id`),
  KEY `idx_admin_audit_log_admin_member_id` (`admin_member_id`),
  KEY `idx_admin_audit_log_action_type` (`action_type`),
  KEY `idx_admin_audit_log_target_type` (`target_type`),
  KEY `idx_admin_audit_log_target_id` (`target_id`),
  KEY `idx_admin_audit_log_created_at` (`created_at`),
  CONSTRAINT `fk_admin_audit_log_member` FOREIGN KEY (`admin_member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_audit_log`
--

LOCK TABLES `admin_audit_log` WRITE;
/*!40000 ALTER TABLE `admin_audit_log` DISABLE KEYS */;
INSERT INTO `admin_audit_log` VALUES (1,NULL,'BOOK_CREATE','BOOK',766,NULL,'{\"isbn\":\"9791699434300\",\"title\":\"실DB REQUIRES_NEW 재현\"}','2026-07-13 09:51:11'),(6,NULL,'BOOK_CREATE','BOOK',852,NULL,'{\"title\":\"실DB REQUIRES_NEW 재현\",\"isbn\":\"9797849647200\"}','2026-07-13 10:06:57'),(11,NULL,'BOOK_CREATE','BOOK',938,NULL,'{\"isbn\":\"9797574467600\",\"title\":\"실DB REQUIRES_NEW 재현\"}','2026-07-13 10:08:16'),(16,NULL,'BOOK_CREATE','BOOK',1024,NULL,'{\"isbn\":\"9797304141800\",\"title\":\"실DB REQUIRES_NEW 재현\"}','2026-07-13 10:24:37'),(21,1,'BOOK_CREATE','BOOK',1110,NULL,'{\"title\":\"놓지 마 과학!\",\"isbn\":\"9791190305310\"}','2026-07-13 10:26:32'),(22,1,'BOOK_CREATE','BOOK',1111,NULL,'{\"title\":\"Why? 소프트웨어와 코딩 \",\"isbn\":\"9788930219556\"}','2026-07-13 10:26:32'),(23,1,'BOOK_CREATE','BOOK',1112,NULL,'{\"title\":\"놓지 마 과학!\",\"isbn\":\"9791168122369\"}','2026-07-13 10:34:05'),(24,1,'BOOK_CREATE','BOOK',1113,NULL,'{\"title\":\"그림 도둑, 준모 \",\"isbn\":\"9788989646075\"}','2026-07-13 10:34:05'),(25,1,'BOOK_CREATE','BOOK',1114,NULL,'{\"title\":\"할머니, 어디 가요? 밤 주우러 간다! :옥이네 가을 이야기 \",\"isbn\":\"9788984286269\"}','2026-07-13 10:34:05'),(26,1,'BOOK_CREATE','BOOK',1115,NULL,'{\"title\":\"Why? 컴퓨터 바이러스 \",\"isbn\":\"9788930206785\"}','2026-07-13 10:34:05'),(27,1,'BOOK_CREATE','BOOK',1116,NULL,'{\"title\":\"게임 없이 못 살아! \",\"isbn\":\"9788959779345\"}','2026-07-13 10:34:05'),(28,1,'BOOK_CREATE','BOOK',1117,NULL,'{\"title\":\"Why? SNS =소셜 네트워크 서비스 \",\"isbn\":\"9788930231633\"}','2026-07-13 10:34:05'),(29,1,'BOOK_CREATE','BOOK',1118,NULL,'{\"title\":\"쿠키런 반짝 미래 과학 상식 :나의 미래 생활 신기한 미래 과학 25 \",\"isbn\":\"9788926386132\"}','2026-07-13 10:34:05'),(30,1,'BOOK_CREATE','BOOK',1119,NULL,'{\"title\":\"Why? 과학사건 \",\"isbn\":\"9788930202794\"}','2026-07-13 10:34:05'),(31,1,'BOOK_CREATE','BOOK',1120,NULL,'{\"title\":\"사라진 물건의 비밀 \",\"isbn\":\"9788949162119\"}','2026-07-13 10:34:05'),(32,1,'BOOK_CREATE','BOOK',1121,NULL,'{\"title\":\"Why? 미래과학 \",\"isbn\":\"9788930202718\"}','2026-07-13 10:34:05'),(33,1,'BOOK_CREATE','BOOK',1122,NULL,'{\"title\":\"(초등학생이 알아야 할) 과학 100가지 \",\"isbn\":\"9791186872802\"}','2026-07-13 10:34:05'),(34,1,'BOOK_CREATE','BOOK',1123,NULL,'{\"title\":\"내일은 로봇왕 :본격 대결 과학로봇 만화\",\"isbn\":\"9788937889165\"}','2026-07-13 10:34:05'),(35,1,'BOOK_CREATE','BOOK',1124,NULL,'{\"title\":\"Why? 컴퓨터 \",\"isbn\":\"9788930202336\"}','2026-07-13 10:34:05'),(36,1,'BOOK_CREATE','BOOK',1125,NULL,'{\"title\":\"Why? 뇌 \",\"isbn\":\"9788930202657\"}','2026-07-13 10:34:05'),(37,1,'BOOK_CREATE','BOOK',1126,NULL,'{\"title\":\"(마녀 위니의)새 컴퓨터\",\"isbn\":\"9788949111278\"}','2026-07-13 10:34:05'),(38,1,'BOOK_CREATE','BOOK',1127,NULL,'{\"title\":\"수학특성화중학교 \",\"isbn\":\"9788958075912\"}','2026-07-13 10:34:05'),(39,1,'BOOK_CREATE','BOOK',1128,NULL,'{\"title\":\"김상욱의 양자 공부 :완전히 새로운 현대 물리학 입문 \",\"isbn\":\"9788983718914\"}','2026-07-13 10:34:06'),(40,1,'BOOK_CREATE','BOOK',1129,NULL,'{\"title\":\"코딩맨 :히어로 액션 코딩 학습 만화\",\"isbn\":\"9791156396635\"}','2026-07-13 10:34:06'),(41,1,'BOOK_CREATE','BOOK',1130,NULL,'{\"title\":\"아웃라이어 :성공의 기회를 발견한 사람들 \",\"isbn\":\"9788934933151\"}','2026-07-13 10:34:06'),(42,1,'BOOK_CREATE','BOOK',1131,NULL,'{\"title\":\"미래가 온다, 뇌과학 \",\"isbn\":\"9791187513810\"}','2026-07-13 10:34:06'),(43,1,'BOOK_CREATE','BOOK',1132,NULL,'{\"title\":\"신기한 스쿨 버스:키즈\",\"isbn\":\"9788949150246\"}','2026-07-13 10:34:06'),(44,1,'BOOK_CREATE','BOOK',1133,NULL,'{\"title\":\"야밤의 공대생 만화 \",\"isbn\":\"9788964620878\"}','2026-07-13 10:34:06'),(45,1,'BOOK_CREATE','BOOK',1134,NULL,'{\"title\":\"받은 편지함:남찬숙 장편 동화\",\"isbn\":\"9788980408443\"}','2026-07-13 10:34:06'),(46,1,'BOOK_CREATE','BOOK',1135,NULL,'{\"title\":\"초예측 :세계 석학 8인에게 인류의 미래를 묻다 \",\"isbn\":\"9788901229614\"}','2026-07-13 10:34:06'),(47,1,'BOOK_CREATE','BOOK',1136,NULL,'{\"title\":\"Why? 게임 과학\",\"isbn\":\"9788930237970\"}','2026-07-13 10:34:06'),(48,1,'BOOK_CREATE','BOOK',1137,NULL,'{\"title\":\"백년 목 :100년 가는 목 만드는 단 하나의 방법 \",\"isbn\":\"9788983718686\"}','2026-07-13 10:34:06'),(49,1,'BOOK_CREATE','BOOK',1138,NULL,'{\"title\":\"Who? 안철수 =Ahn Cheolsoo \",\"isbn\":\"9791156394211\"}','2026-07-13 10:34:06'),(50,1,'BOOK_CREATE','BOOK',1139,NULL,'{\"title\":\"구스범스\",\"isbn\":\"9788965481676\"}','2026-07-13 10:34:06'),(51,1,'BOOK_CREATE','BOOK',1140,NULL,'{\"title\":\"미래가 온다 인공지능 \",\"isbn\":\"9791187513858\"}','2026-07-13 10:34:06'),(52,1,'BOOK_CREATE','BOOK',1141,NULL,'{\"title\":\"(퀴즈!) 과학상식 :미래 과학 \",\"isbn\":\"9791170181699\"}','2026-07-13 10:34:06'),(53,1,'BOOK_CREATE','BOOK',1142,NULL,'{\"title\":\"Why? 암호 화폐와 블록체인 \",\"isbn\":\"9788930237925\"}','2026-07-13 10:35:45'),(54,1,'BOOK_CREATE','BOOK',1143,NULL,'{\"title\":\"Why? 정보통신 - 2판\",\"isbn\":\"9788930202671\"}','2026-07-13 10:35:45'),(55,1,'BOOK_CREATE','BOOK',1144,NULL,'{\"title\":\"(과학이 톡톡 쌓이다!) 사이다 =Sci-da\",\"isbn\":\"9791185402420\"}','2026-07-13 10:35:45'),(56,1,'BOOK_CREATE','BOOK',1145,NULL,'{\"title\":\"생각하지 않는 사람들 :인터넷이 우리의 뇌 구조를 바꾸고 있다! \",\"isbn\":\"9788935208647\"}','2026-07-13 10:35:45'),(57,1,'BOOK_CREATE','BOOK',1146,NULL,'{\"title\":\"분수와 소수\",\"isbn\":\"9791186872536\"}','2026-07-13 10:35:45'),(58,1,'BOOK_CREATE','BOOK',1147,NULL,'{\"title\":\"휴대폰 전쟁 \",\"isbn\":\"9788971849866\"}','2026-07-13 10:35:45'),(59,1,'BOOK_CREATE','BOOK',1148,NULL,'{\"title\":\"팜 :기발한 상상력이 가득한 판타지 코딩과학동화\",\"isbn\":\"9791165211004\"}','2026-07-13 10:35:45'),(60,1,'BOOK_CREATE','BOOK',1149,NULL,'{\"title\":\"도티&잠뜰 코딩\",\"isbn\":\"9791133442003\"}','2026-07-13 10:35:45'),(61,1,'BOOK_CREATE','BOOK',1150,NULL,'{\"title\":\"신기한 스쿨 버스:키즈\",\"isbn\":\"9788949150253\"}','2026-07-13 10:35:45'),(62,1,'BOOK_CREATE','BOOK',1151,NULL,'{\"title\":\"(쉽고 재밌는) 컴퓨터와 코딩 \",\"isbn\":\"9791186872529\"}','2026-07-13 10:35:45');
/*!40000 ALTER TABLE `admin_audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admin_statistic`
--

DROP TABLE IF EXISTS `admin_statistic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_statistic` (
  `admin_stat_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `period_type` varchar(20) NOT NULL,
  `age_band` varchar(20) NOT NULL DEFAULT 'ALL',
  `period_start_date` date NOT NULL,
  `period_end_date` date NOT NULL,
  `total_member_count` int unsigned NOT NULL DEFAULT '0',
  `sanctioned_member_count` int unsigned NOT NULL DEFAULT '0',
  `active_book_count` int unsigned NOT NULL DEFAULT '0',
  `total_rental_count` int unsigned NOT NULL DEFAULT '0',
  `total_read_book_count` int unsigned NOT NULL DEFAULT '0',
  `free_rental_count` int unsigned NOT NULL DEFAULT '0',
  `paid_rental_count` int unsigned NOT NULL DEFAULT '0',
  `total_payment_amount` bigint NOT NULL DEFAULT '0',
  `review_count` int unsigned NOT NULL DEFAULT '0',
  `report_count` int unsigned NOT NULL DEFAULT '0',
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`admin_stat_id`),
  UNIQUE KEY `uk_admin_statistic_period_type_period_start_date_age_band` (`period_type`,`period_start_date`,`age_band`),
  CONSTRAINT `chk_admin_statistic_age_band` CHECK ((`age_band` in (_utf8mb4'ALL',_utf8mb4'10S',_utf8mb4'20S',_utf8mb4'30S',_utf8mb4'40S',_utf8mb4'50_PLUS'))),
  CONSTRAINT `chk_admin_statistic_period_type` CHECK ((`period_type` in (_utf8mb4'WEEKLY',_utf8mb4'MONTHLY',_utf8mb4'YEARLY',_utf8mb4'TOTAL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_statistic`
--

LOCK TABLES `admin_statistic` WRITE;
/*!40000 ALTER TABLE `admin_statistic` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin_statistic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance`
--

DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `attendance_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `attendance_date` date NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`attendance_id`),
  UNIQUE KEY `uk_attendance_member_id_attendance_date` (`member_id`,`attendance_date`),
  CONSTRAINT `fk_attendance_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance`
--

LOCK TABLES `attendance` WRITE;
/*!40000 ALTER TABLE `attendance` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `author`
--

DROP TABLE IF EXISTS `author`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `author` (
  `author_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `author_name` varchar(100) NOT NULL,
  `author_intro` text,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`author_id`),
  KEY `idx_author_author_name` (`author_name`),
  KEY `idx_author_status` (`status`),
  CONSTRAINT `chk_author_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=329 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `author`
--

LOCK TABLES `author` WRITE;
/*!40000 ALTER TABLE `author` DISABLE KEYS */;
INSERT INTO `author` VALUES (9,'로버트 C. 마틴',NULL,'ACTIVE','2026-07-11 10:42:27'),(10,'이해영',NULL,'ACTIVE','2026-07-11 10:42:27'),(11,'테스트저자',NULL,'ACTIVE','2026-07-11 15:25:46'),(84,'한강',NULL,'ACTIVE','2026-07-11 16:40:58'),(265,'신태훈',NULL,'ACTIVE','2026-07-13 10:26:32'),(266,'나승훈',NULL,'ACTIVE','2026-07-13 10:26:32'),(267,'조영선',NULL,'ACTIVE','2026-07-13 10:26:32'),(268,'이영호',NULL,'ACTIVE','2026-07-13 10:26:32'),(269,'오승희',NULL,'ACTIVE','2026-07-13 10:34:05'),(270,'최정인',NULL,'ACTIVE','2026-07-13 10:34:05'),(271,'조혜란 그리고 씀',NULL,'ACTIVE','2026-07-13 10:34:05'),(272,'안미연 글',NULL,'ACTIVE','2026-07-13 10:34:05'),(273,'이용규',NULL,'ACTIVE','2026-07-13 10:34:05'),(274,'조주희',NULL,'ACTIVE','2026-07-13 10:34:05'),(275,'이태영',NULL,'ACTIVE','2026-07-13 10:34:05'),(276,'박상욱',NULL,'ACTIVE','2026-07-13 10:34:05'),(277,'이태훈',NULL,'ACTIVE','2026-07-13 10:34:05'),(278,'이분희 글',NULL,'ACTIVE','2026-07-13 10:34:05'),(279,'이덕화',NULL,'ACTIVE','2026-07-13 10:34:05'),(280,'알렉스 프리스',NULL,'ACTIVE','2026-07-13 10:34:05'),(281,'최새미',NULL,'ACTIVE','2026-07-13 10:34:05'),(282,'포도알친구',NULL,'ACTIVE','2026-07-13 10:34:05'),(283,'홍종현',NULL,'ACTIVE','2026-07-13 10:34:05'),(284,'그림수레',NULL,'ACTIVE','2026-07-13 10:34:05'),(285,'남춘자',NULL,'ACTIVE','2026-07-13 10:34:05'),(286,'박종성',NULL,'ACTIVE','2026-07-13 10:34:05'),(287,'코키 폴',NULL,'ACTIVE','2026-07-13 10:34:05'),(288,'노은정',NULL,'ACTIVE','2026-07-13 10:34:05'),(289,'이윤원',NULL,'ACTIVE','2026-07-13 10:34:05'),(290,'김주희',NULL,'ACTIVE','2026-07-13 10:34:05'),(291,'김상욱',NULL,'ACTIVE','2026-07-13 10:34:06'),(292,'송아론',NULL,'ACTIVE','2026-07-13 10:34:06'),(293,'김기수',NULL,'ACTIVE','2026-07-13 10:34:06'),(294,'말콤 글래드웰 [지음]',NULL,'ACTIVE','2026-07-13 10:34:06'),(295,'노정태',NULL,'ACTIVE','2026-07-13 10:34:06'),(296,'김성화',NULL,'ACTIVE','2026-07-13 10:34:06'),(297,'조승연',NULL,'ACTIVE','2026-07-13 10:34:06'),(298,'조애너 콜 글',NULL,'ACTIVE','2026-07-13 10:34:06'),(299,'브루스 디건',NULL,'ACTIVE','2026-07-13 10:34:06'),(300,'맹기완',NULL,'ACTIVE','2026-07-13 10:34:06'),(301,'남찬숙',NULL,'ACTIVE','2026-07-13 10:34:06'),(302,'황보순희',NULL,'ACTIVE','2026-07-13 10:34:06'),(303,'유발 하라리',NULL,'ACTIVE','2026-07-13 10:34:06'),(304,'정현옥',NULL,'ACTIVE','2026-07-13 10:34:06'),(305,'윤형섭',NULL,'ACTIVE','2026-07-13 10:34:06'),(306,'정선근',NULL,'ACTIVE','2026-07-13 10:34:06'),(307,'최재훈',NULL,'ACTIVE','2026-07-13 10:34:06'),(308,'정석호',NULL,'ACTIVE','2026-07-13 10:34:06'),(309,'R. L. 스타인',NULL,'ACTIVE','2026-07-13 10:34:06'),(310,'신인수',NULL,'ACTIVE','2026-07-13 10:34:06'),(311,'이철민',NULL,'ACTIVE','2026-07-13 10:34:06'),(312,'차현진',NULL,'ACTIVE','2026-07-13 10:34:06'),(313,'이준희',NULL,'ACTIVE','2026-07-13 10:35:45'),(314,'정원영',NULL,'ACTIVE','2026-07-13 10:35:45'),(315,'김하나',NULL,'ACTIVE','2026-07-13 10:35:45'),(316,'니콜라스 카',NULL,'ACTIVE','2026-07-13 10:35:45'),(317,'최지향',NULL,'ACTIVE','2026-07-13 10:35:45'),(318,'로지 디킨스',NULL,'ACTIVE','2026-07-13 10:35:45'),(319,'베네데타 죠프레',NULL,'ACTIVE','2026-07-13 10:35:45'),(320,'엔리카 루시나',NULL,'ACTIVE','2026-07-13 10:35:45'),(321,'로이스 페터슨',NULL,'ACTIVE','2026-07-13 10:35:45'),(322,'고수미',NULL,'ACTIVE','2026-07-13 10:35:45'),(323,'홍지연',NULL,'ACTIVE','2026-07-13 10:35:45'),(324,'지문',NULL,'ACTIVE','2026-07-13 10:35:45'),(325,'유경원',NULL,'ACTIVE','2026-07-13 10:35:45'),(326,'최대식',NULL,'ACTIVE','2026-07-13 10:35:45'),(327,'로지 디킨스 글',NULL,'ACTIVE','2026-07-13 10:35:45'),(328,'쇼 닐센',NULL,'ACTIVE','2026-07-13 10:35:45');
/*!40000 ALTER TABLE `author` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book`
--

DROP TABLE IF EXISTS `book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book` (
  `book_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `publisher_id` bigint unsigned DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `isbn` char(13) DEFAULT NULL,
  `cover_image_url` varchar(500) DEFAULT NULL,
  `pub_date` date DEFAULT NULL,
  `rental_type` varchar(20) NOT NULL,
  `rental_price` int NOT NULL DEFAULT '0',
  `default_rental_days` int unsigned NOT NULL,
  `total_pages` int unsigned NOT NULL DEFAULT '0',
  `rental_count` int unsigned NOT NULL DEFAULT '0',
  `average_rating` decimal(3,2) NOT NULL DEFAULT '0.00',
  `review_count` int unsigned NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`book_id`),
  UNIQUE KEY `uk_book_isbn` (`isbn`),
  KEY `idx_book_title` (`title`),
  KEY `idx_book_publisher_id` (`publisher_id`),
  KEY `idx_book_rental_type` (`rental_type`),
  KEY `idx_book_status` (`status`),
  KEY `idx_book_rental_count` (`rental_count`),
  KEY `idx_book_average_rating` (`average_rating`),
  KEY `idx_book_review_count` (`review_count`),
  CONSTRAINT `fk_book_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `publisher` (`publisher_id`),
  CONSTRAINT `chk_book_rental_type` CHECK ((`rental_type` in (_utf8mb4'FREE',_utf8mb4'PAID'))),
  CONSTRAINT `chk_book_status` CHECK ((`status` in (_utf8mb4'AVAILABLE',_utf8mb4'HIDDEN',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=1152 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book`
--

LOCK TABLES `book` WRITE;
/*!40000 ALTER TABLE `book` DISABLE KEYS */;
INSERT INTO `book` VALUES (332,97,'소년이 온다','9788936434120','https://image.aladin.co.kr/product/4086/97/cover200/8936434128_2.jpg','2014-05-19','FREE',0,7,2,0,0.00,0,'AVAILABLE','2026-07-11 16:40:57',NULL),(1110,295,'놓지 마 과학!','9791190305310','https://image.aladin.co.kr/product/21502/77/cover200/k742636192_1.jpg','2019-11-08','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:26:32',NULL),(1111,296,'Why? 소프트웨어와 코딩 ','9788930219556','https://image.aladin.co.kr/product/7153/70/cover200/8930219551_1.jpg','2015-12-15','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:26:32',NULL),(1112,295,'놓지 마 과학!','9791168122369','https://image.aladin.co.kr/product/29305/35/cover200/k992837600_1.jpg','2022-05-06','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1113,297,'그림 도둑, 준모 ','9788989646075','https://image.aladin.co.kr/product/41/65/cover200/8989646073_2.jpg','2003-05-30','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1114,298,'할머니, 어디 가요? 밤 주우러 간다! :옥이네 가을 이야기 ','9788984286269','https://image.aladin.co.kr/product/773/37/cover200/8984286265_1.jpg','2010-08-31','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1115,296,'Why? 컴퓨터 바이러스 ','9788930206785','https://image.aladin.co.kr/product/4458/69/cover200/8930206786_1.jpg','2014-08-15','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1116,299,'게임 없이 못 살아! ','9788959779345','https://image.aladin.co.kr/product/773/82/cover200/8959779342_1.jpg','2010-08-30','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1117,296,'Why? SNS =소셜 네트워크 서비스 ','9788930231633','https://image.aladin.co.kr/product/3642/25/cover200/8930231632_1.jpg','2014-01-10','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1118,300,'쿠키런 반짝 미래 과학 상식 :나의 미래 생활 신기한 미래 과학 25 ','9788926386132','https://image.aladin.co.kr/product/8590/2/cover200/8926386139_1.jpg','2016-06-24','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1119,296,'Why? 과학사건 ','9788930202794','https://image.aladin.co.kr/product/716/61/cover200/8930202799_1.jpg','2008-12-31','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1120,301,'사라진 물건의 비밀 ','9788949162119','https://image.aladin.co.kr/product/28343/5/cover200/8949162113_1.jpg','2021-11-15','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1121,296,'Why? 미래과학 ','9788930202718','https://image.aladin.co.kr/product/716/60/cover200/8930202713_1.jpg','2008-07-21','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1122,302,'(초등학생이 알아야 할) 과학 100가지 ','9791186872802','https://image.aladin.co.kr/product/11256/10/cover200/k642531791_1.jpg','2017-06-29','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1123,303,'내일은 로봇왕 :본격 대결 과학로봇 만화','9788937889165','https://image.aladin.co.kr/product/11879/14/cover200/8937889161_1.jpg','2017-09-27','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1124,296,'Why? 컴퓨터 ','9788930202336','https://image.aladin.co.kr/product/716/62/cover200/8930202330_1.jpg','2009-02-25','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1125,296,'Why? 뇌 ','9788930202657','https://image.aladin.co.kr/product/716/73/cover200/8930202659_1.jpg','2008-01-10','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1126,301,'(마녀 위니의)새 컴퓨터','9788949111278','https://image.aladin.co.kr/product/51/20/cover200/k352535735_1.jpg','2004-08-01','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1127,304,'수학특성화중학교 ','9788958075912','https://image.aladin.co.kr/product/7236/84/cover200/8958075910_1.jpg','2015-12-24','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:05',NULL),(1128,305,'김상욱의 양자 공부 :완전히 새로운 현대 물리학 입문 ','9788983718914','https://image.aladin.co.kr/product/11380/16/cover200/s162534273_1.jpg','2017-12-08','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1129,306,'코딩맨 :히어로 액션 코딩 학습 만화','9791156396635','https://image.aladin.co.kr/product/12931/52/cover200/k472532488_1.jpg','2018-01-26','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1130,307,'아웃라이어 :성공의 기회를 발견한 사람들 ','9788934933151','https://image.aladin.co.kr/product/301/7/cover200/8934933151_3.jpg','2009-01-15','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1131,308,'미래가 온다, 뇌과학 ','9791187513810','https://image.aladin.co.kr/product/20182/47/cover200/k712635640_1.jpg','2019-08-05','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1132,301,'신기한 스쿨 버스:키즈','9788949150246','https://image.aladin.co.kr/product/30/16/cover200/8949150247_2.jpg','2001-11-12','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1133,309,'야밤의 공대생 만화 ','9788964620878','https://image.aladin.co.kr/product/10044/20/cover200/8964620879_2.jpg','2017-07-07','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1134,310,'받은 편지함:남찬숙 장편 동화','9788980408443','https://image.aladin.co.kr/product/57/36/cover200/8980408447_1.jpg','2005-07-15','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1135,311,'초예측 :세계 석학 8인에게 인류의 미래를 묻다 ','9788901229614','https://image.aladin.co.kr/product/18116/28/cover200/8901229617_1.jpg','2019-02-08','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1136,296,'Why? 게임 과학','9788930237970','https://image.aladin.co.kr/product/25681/18/cover200/8930237975_1.jpg','2020-11-10','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1137,305,'백년 목 :100년 가는 목 만드는 단 하나의 방법 ','9788983718686','https://image.aladin.co.kr/product/12388/98/cover200/8983718684_1.jpg','2017-11-30','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1138,312,'Who? 안철수 =Ahn Cheolsoo ','9791156394211','https://image.aladin.co.kr/product/10439/50/cover200/k992536391_1.jpg','2017-03-06','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1139,313,'구스범스','9788965481676','https://image.aladin.co.kr/product/4685/6/cover200/8965481678_1.jpg','2014-09-25','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1140,308,'미래가 온다 인공지능 ','9791187513858','https://image.aladin.co.kr/product/21418/67/cover200/k102636896_2.jpg','2019-11-20','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1141,314,'(퀴즈!) 과학상식 :미래 과학 ','9791170181699','https://image.aladin.co.kr/product/693/70/cover200/k682434891_1.jpg','2015-11-30','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:34:06',NULL),(1142,296,'Why? 암호 화폐와 블록체인 ','9788930237925','https://image.aladin.co.kr/product/21413/30/cover200/8930237924_1.jpg','2019-10-25','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1143,296,'Why? 정보통신 - 2판','9788930202671','https://image.aladin.co.kr/product/716/69/cover200/8930202675_1.jpg','2008-01-10','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1144,315,'(과학이 톡톡 쌓이다!) 사이다 =Sci-da','9791185402420','https://image.aladin.co.kr/product/28170/88/cover200/k922835563_2.jpg','2021-10-25','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1145,316,'생각하지 않는 사람들 :인터넷이 우리의 뇌 구조를 바꾸고 있다! ','9788935208647','https://image.aladin.co.kr/product/21025/75/cover200/k282636570_1.jpg','2019-10-17','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1146,317,'분수와 소수','9791186872536','https://image.aladin.co.kr/product/8666/19/cover200/k782535080_1.jpg','2016-07-05','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1147,318,'휴대폰 전쟁 ','9788971849866','https://image.aladin.co.kr/product/3298/39/cover200/897184986x_1.jpg','2013-10-30','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1148,319,'팜 :기발한 상상력이 가득한 판타지 코딩과학동화','9791165211004','https://image.aladin.co.kr/product/23590/25/cover200/k542638327_1.jpg','2020-04-02','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1149,320,'도티&잠뜰 코딩','9791133442003','https://image.aladin.co.kr/product/10349/23/cover200/k632536680_1.jpg','2017-02-20','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1150,301,'신기한 스쿨 버스:키즈','9788949150253','https://image.aladin.co.kr/product/30/16/cover200/8949150255_2.jpg','2001-11-12','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL),(1151,302,'(쉽고 재밌는) 컴퓨터와 코딩 ','9791186872529','https://image.aladin.co.kr/product/8665/86/cover200/k562535080_1.jpg','2016-07-05','PAID',5000,14,3,0,0.00,0,'AVAILABLE','2026-07-13 10:35:45',NULL);
/*!40000 ALTER TABLE `book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_author`
--

DROP TABLE IF EXISTS `book_author`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_author` (
  `book_id` bigint unsigned NOT NULL,
  `author_id` bigint unsigned NOT NULL,
  `author_order` int unsigned NOT NULL DEFAULT '1',
  PRIMARY KEY (`book_id`,`author_id`),
  KEY `idx_book_author_author_id` (`author_id`),
  CONSTRAINT `fk_book_author_author` FOREIGN KEY (`author_id`) REFERENCES `author` (`author_id`),
  CONSTRAINT `fk_book_author_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_author`
--

LOCK TABLES `book_author` WRITE;
/*!40000 ALTER TABLE `book_author` DISABLE KEYS */;
INSERT INTO `book_author` VALUES (332,84,1),(1110,265,1),(1110,266,2),(1111,267,1),(1111,268,2),(1112,265,1),(1112,266,2),(1113,269,1),(1113,270,2),(1114,271,1),(1115,267,1),(1115,268,2),(1116,272,1),(1116,273,2),(1117,267,1),(1117,268,2),(1118,274,1),(1118,275,2),(1119,276,1),(1119,277,2),(1120,278,1),(1120,279,2),(1121,267,1),(1121,268,2),(1122,280,1),(1122,281,2),(1123,282,1),(1123,283,2),(1124,267,1),(1124,284,2),(1125,285,1),(1125,286,2),(1126,287,1),(1126,288,2),(1127,289,1),(1127,290,2),(1128,291,1),(1129,292,1),(1129,293,2),(1130,294,1),(1130,295,2),(1131,296,1),(1131,297,2),(1132,298,1),(1132,299,2),(1133,300,1),(1134,301,1),(1134,302,2),(1135,303,1),(1135,304,2),(1136,267,1),(1136,268,2),(1136,305,3),(1137,306,1),(1138,307,1),(1138,308,2),(1139,309,1),(1139,310,2),(1140,296,1),(1140,311,2),(1141,312,1),(1142,267,1),(1142,313,2),(1143,267,1),(1144,314,1),(1144,315,2),(1145,316,1),(1145,317,2),(1146,318,1),(1146,319,2),(1146,320,3),(1147,321,1),(1147,322,2),(1148,323,1),(1148,324,2),(1149,325,1),(1149,326,2),(1150,298,1),(1150,299,2),(1151,327,1),(1151,328,2);
/*!40000 ALTER TABLE `book_author` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_category`
--

DROP TABLE IF EXISTS `book_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_category` (
  `book_id` bigint unsigned NOT NULL,
  `category_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`book_id`,`category_id`),
  KEY `idx_book_category_category_id` (`category_id`),
  CONSTRAINT `fk_book_category_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_book_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_category`
--

LOCK TABLES `book_category` WRITE;
/*!40000 ALTER TABLE `book_category` DISABLE KEYS */;
INSERT INTO `book_category` VALUES (332,160),(1110,464),(1111,464),(1115,464),(1117,464),(1123,464),(1129,464),(1148,464),(1149,464),(1151,464),(1112,465),(1118,465),(1121,465),(1122,465),(1124,465),(1132,465),(1136,465),(1140,465),(1142,465),(1143,465),(1144,465),(1150,465),(1113,467),(1114,467),(1116,467),(1120,467),(1134,467),(1119,469),(1125,470),(1131,470),(1126,474),(1127,477),(1147,477),(1128,480),(1130,483),(1133,484),(1135,486),(1137,489),(1138,491),(1139,492),(1141,493),(1145,496),(1146,497);
/*!40000 ALTER TABLE `book_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_detail`
--

DROP TABLE IF EXISTS `book_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_detail` (
  `book_detail_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `book_id` bigint unsigned NOT NULL,
  `description` text,
  `table_of_contents` text,
  `publisher_review` text,
  `detail_content` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`book_detail_id`),
  UNIQUE KEY `uk_book_detail_book_id` (`book_id`),
  CONSTRAINT `fk_book_detail_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_detail`
--

LOCK TABLES `book_detail` WRITE;
/*!40000 ALTER TABLE `book_detail` DISABLE KEYS */;
INSERT INTO `book_detail` VALUES (36,332,'5·18 광주민주화운동을 다룬 한강 장편소설',NULL,NULL,NULL,'2026-07-11 16:40:57',NULL),(92,1110,'정신이는 최고의 과학 선생님!\n“아직도 〈놓지 마 과학!〉 안 보셨어요?”\n\n인기 절정의 웹툰 〈놓지 마 정신줄!〉의 작가들이 만든 과학 학습 만화 〈놓지 마 과학!〉 12권이 출간되었다. 1차분 1~5권과 2차분 6~10권에 이어 3차분 11~15권의 두 번째 권인 12권이 출간된 것이다. 〈놓지 마 과학!〉 시리즈는 1권이 2016년 8월에 출간된 이후, 매번 새 책이 나올 때마다 학습 만화 베스트셀러로 자리 잡으며 인기를 더하고 있다. 이 때문에 다른 책은 몰라도 이 책만은 꼭 본다는 충성도 높은 어린이 독자들의 수가 점점 늘어 가고 있으며, 새 책의 출간 시기를 묻는 문의 전화 또한 잇따른다고 한다. \n\n〈놓지 마 정신줄!〉은 2009년 8월 연재를 시작해 현재까지 11년째 이어 오며 조회 수 28억 뷰가 넘는 초인기 웹툰으로 성장했다. 또한 어린이 독자들의 사랑을 바탕으로 방송 애니메이션으로 만들어지기도 했다. 〈놓지 마 과학!〉 시리즈는 〈놓지 마 정신줄!〉의 캐릭터들이 등장하여 생활 속에서 생기는 과학적 질문들을 엉뚱하고도 기발한 전개를 통해 자연스럽게 풀어 주고 또한 예상을 빗나가는 결말로 웃음을 터뜨리게 만든다.',NULL,NULL,NULL,'2026-07-13 10:26:32',NULL),(93,1111,'논리력 사고력을 키우는 과학 만화!\n\n흥미진진한 모험 이야기에 소프트웨어와 코딩에 관한 내용을 재미있게 녹여낸 『Why? 소프트웨어와 코딩』. 앞으로 초등학교와 중학교에서 코딩 수업이 의무화된다. 하지만 C언어나 자바(Java)처럼 복잡한 명령어를 직접 입력해야 하는 프로그래밍 언어는 어린이들이 접근하기 어렵다. 그래서 등장한 것이 바로 스크래치나 엔트리 같은 블록형 프로그래밍 언어이다. 코딩은 단지 컴퓨터 프로그램을 움직이게 하는 데 그치는 것이 아니라 생각하는 방법을 찾고, 논리적인 사고력을 기르며, 문제를 해결하는 능력을 키워 준다. 이 책이 코딩에 대한 흥미를 유발하고 잠재력을 깨우는 좋은 자극이 될 것이다.',NULL,NULL,NULL,'2026-07-13 10:26:32',NULL),(94,1112,'100만 부를 돌파한 대한민국 대표 학습 만화! \nCSI 과학수사대를 방불케 하는 증거 찾기X최신 과학 정보X 권위 있는 과학 전문가 감수!\n\n2009~2019년 연재된 웹툰 〈놓지 마 정신줄!〉은 조회 수 28억 뷰가 넘는 초인기 웹툰으로 초등학생들의 절대적인 지지와 사랑을 받았다. 이후 애니메이션을 비롯해 웹드라마까지 만들어지며 나이와 연령을 불문하고 대중에게 사랑을 받는 콘텐츠로 자리매김했다. 이 인기에 힘입어 〈놓지 마 과학!〉 학습 만화 시리즈는 총 16권이 출간되었으며, ‘100만 부 판매를 돌파한 대한민국 대표 학습 만화의 결정판’이라 불리고 있다. \n드디어 《놓지 마 과학! 17권》이 발매되었다. 이번 권에서는 특별한 의뢰인 박순이가 사연을 들고 탐정사무소로 찾아온다. 박순이에게는 어떤 사연이 있는 걸까? 박순이네 아빠는 지구 온난화 위기를 전 세계에 알리겠다며 갑자기 투발루로 떠난다. 그런데 엉겁결에 모카노로 가는 비행기를 타게 되고, 거기다 내뿜내뿜 화산까지 분출해 강제로 모카노에서 지내게 된다. 박순이네 아빠는 모카노 투어 중 보물을 발견하게 되어 모카노 왕국 왕비로부터 초특급 보물 세트를 받는다. 이 보물을 둘러싸고 계속 이상한 사건 사고들이 벌어지는데……. \n《놓지 마 과학! 17권》에는 스릴감 넘치는 증거 찾기 과정이 담겨 있다. 또한 ‘정신이가 만난 과학자: 앙투안 로랑 라부아지에’ 편의 인터뷰가 실려 있으며, 하나의 과학 주제를 집중해서 샅샅이 살펴볼 수 있는 ‘정신이의 과학 X-Files’ 코너에서는 화산에 대한 과학 정보를 사진과 일러스트와 함께 풍부하게 구성해 독자들에게 볼거리를 제공한다. 본문에 담긴 학습 내용을 ‘정신이와 실력 뽐내기 퀴즈’ 코너를 통해 한 번 더 점검해 볼 수 있다. 특히 이번 권부터는 권위 있는 과학 전문가인 서울대학교 화학교육과 홍훈기 교수의 감수를 받아 책의 완성도를 더 높였다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(95,1113,'평범한 아이 준모는 상을 한번 받아서 엄마를 기쁘게 하는 것이 소원이다. 엄마 역시 준모의 \'소질\'을 찾아 주지 못해 안달이다. 준모는 낙서처럼 그린 그림 때문에 \'소질\'을 발견(?)하게 되고, 그후 준모는 각종 그리기 대회를 겨냥해 맹연습에 들어간다. 그러나 상은 좀처럼 주워지지 않는다. 그리고 교내 불조심 그림 그리기 대회날, 준모는 작은 실수를 하게 된다. 하지만 그 실수가 큰 오해를 불러오는데...\n\n아이들을 무한 경쟁으로 내모는 어른들의 욕심과, 그속에서 상처받는 아이의 마음을 그리고 있는 이책은 우리 모두가 상을 받지 않아도 특별한 재능이 없어도 있는 그대로의 특별한 아이임을 알려주고 있다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(96,1114,'옥이도 할머니도 바쁜 가을이 돌아왔어요\n\n봄, 여름, 가을, 겨울 자연 속에서 착하게, 신나게, 예쁘게 살아가는 「옥이네 이야기」 ‘가을’ 편 『할머니 어디 가요? 밤 주우러 간다!』. 끼룩끼룩 기러기 우는 가을날에 맛난 반찬 귀한 반찬 하러 들로 산으로 갯가로 뛰어다니는 일곱 살 옥이와 옥이 할머니의 이야기다. 할머니와 함께 덤불을 헤치고 밤을 주운 옥이는, 주운 밤으로 약밥, 송편 만들어 시장에 팔러 나간다. 그리고 할머니가 사주신 고운 한복 입고 할머니, 아빠, 엄마 모두 모여 차례도 지내고 맛있는 음식을 먹으며 추석을 보낸다. 자연 속에서 신나게 뛰어노는 옥이와 정겨운 시골 사람들의 이야기를 유쾌하고 발랄한 문장과 그림 속에 담아내 독자들을 이야기 속으로 빠져들게 한다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(97,1115,'컴퓨터 바이러스로부터 우리의 안전을 지켜내자!\n\n아이들에게 과학에 대한 놀라운 지식을 전해주는 「초등과학학습만화」 제57권 『Why? 컴퓨터 바이러스』. 이 시리즈는 아이들이 딱딱하고 어려운 과학을 쉽고 재미있게 만나도록 엮었습니다. 만화 페이지와 정보 페이지가 어우러져 다양한 과학적 정보를 쉽고 일목요연하게 배워나갈 수 있습니다. 특히 모든 과학의 출발인 호기심과 궁금증을 시원하게 풀어줍니다.  \n\n인터넷을 사용하는 사람들이라면 한 번쯤 들어봤을 컴퓨터 바이러스에 대해 소개했습니다. 컴퓨터 바이러스의 원인이 무엇인지, 어떻게 감염되는지, 종류에 따라 어떤 증상이 나타나는 지 등을 알려줍니다. 더불어 다소 딱딱할 수 있는 IT용어와 개념들을 아이들이 쉽게 이해할 수 있도록 설명하고, 사이버 상에서 이루어지는 무분별한 행동들이 어떤 결과를 초래하는지에 대한 윤리적인 성찰도 담아냈습니다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(98,1116,'게임에게서 \'시간의 주인공\'의 자리를 되찾자!\n\n저학년 아이들을 위한 「좋은책어린이 저학년문고」 제25권 『게임 없이 못 살아』. 날마다 게임에 푹 빠져 사는 소년 \'시준이\'의 이야기를 담아낸 장편동화다. 엄마, 아빠와의 약속 시간도 지키지 않고 게임에만 매달리다가, 게임 세계로 들어가 이무기와 싸우게 된 시준이의 환상적 모험 속으로 아이들을 초대하고 있다. 게임만 하면 시간이 흘러가는지도 모르고 정신을 팔아버리는 아이들이 \'시간의 주인공\'이 되도록 이끈다. 게임에 끌려다니지 않고 스스로 조절하며 즐기도록 인도하는 것이다. 엄마, 아빠에게는 게임을 둘러싼 잔소리 전쟁을 끝내는 비결을 가르쳐준다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(99,1117,'아이들의 교양 지식을 키우다!\n\n인문사회 교양만화 『Why SNS』. 국어ㆍ수학ㆍ사회ㆍ예체능 과목에 대한 학습 능력을 키우고 서술형 문제로 논리력과 창의력을 길러주는 책입니다. 각각의 주제는 초ㆍ중등학교 교과 과정과 연계하여 학습에 도움이 되도록 했습니다. 각 권마다 해당 분야의 전문학자ㆍ교수ㆍ연구원들의 세심한 감수로 내용의 정확성을 높였습니다. 어려운 전문 용어나 꼭 필요한 한자어는 풀이를 따로 두어 어린 독자들의 지적 호기심을 풀어줬습니다.\n\nSNS는 소셜 네트워크 서비스의 약자입니다. 인터넷 통신망을 통해 새로운 사람과 관계를 쌓거나 이미 알고 있던 사람과의 관계를 돈독하게 만들어줍니다. SNS가 어떻게 발전해 왔으며 우리에게 어떤 영향을 미치고 있는지 다뤘습니다. 더불어 앞으로 SNS를 어떻게 이용해야 행복한 미래를 만들 수 있는지 고민해볼 시간을 제공할 것입니다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(100,1118,'웃으면서 배우는 과학 상식!\n\n일상생활에서 생겨나는 무궁무진한 호기심들을 과학적 원리를 이용해 간단명료하게 풀어내는 「쿠키런 펀펀 상식 시리즈」 제12권 『쿠키런 반짝미래 과학 상식』. 곧 다가올 미래 사회에서 우리가 어떤 환경에서 어떤 모습으로 살아가고 있을지를 쿠키들이 펼치는 25개의 에피소드를 통해 보여 줍니다. 그림을 통해 쉽고 재미있게 풀어 낸 과학 상식은 감수 선생님의 도움으로 더욱 정확도를 높였습니다. 귀여운 쿠키들의 황당하고 재미있는 에피소드를 읽다 보면 자연스럽게 학습 효과가 높아집니다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(101,1119,'전문가가 꼼꼼히 감수한 과학 지식을 어린이들이 좋아하는 올컬러 만화로 재미나게 풀어낸 「초등과학학습만화 WHY」시리즈 『과학사건』. 한국과학창의재단에서 발표한 \'인류 100대 과학 사건\' 중에서 밀접한 관련을 가진 사건을 모아, 만화와 사진, 세밀화로 재구성해 어린이들이 과학의 역사를 한눈에 볼 수 있도록 꾸몄다. 재미난 이야기와 익살맞은 그림이 충실히 담겨있어 과학 지식과 조화를 잘 이루어 재미나면서도 깊이 있는 과학을 접할 수 있다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(102,1120,'이건 잃어버린 게 아니라 사라진 거야! \n툭하면 잃어버리기 잘하는 기찬이의 자기 물건 찾기 대작전\n\n국내 20만 부 판매 『한밤중 달빛 식당』 이분희 작가의 신작 『사라진 물건의 비밀』이 출간되었다. 이분희 작가는 비룡소 문학상과 황금도깨비상을 같은 해에 수상하고 수상작 모두 출간 이후 베스트셀러로 올라 신인 작가로서 단번에 뜨거운 주목을 받았다. 『사라진 물건의 비밀』은 이분희 작가가 오랜만에 독자들에게 선보이는 작품으로 작가 특유의 글맛과 입담이 살아 있는 따뜻한 판타지 동화다. \n\n자기 물건을 잘 간수하지 못해 엄마에게 늘 혼나는 기찬이는 어느 날 이번만은 자기가 잃어버린 게 아니라 물건이 사라지고 있다고 의심하며 그 사라진 물건들을 찾아 나서는 모험을 담았다. 물건을 하나 잃어버릴 때마다 일주일간 컴퓨터 사용 금지라는 무시무시한 ‘엄마법’이 생기자마자 벌어진 일로, 기찬이는 사라진 물건에 대한 단서들을 모으며 범인에 대한 윤곽을 잡아나간다. 기찬이가 단서를 하나하나 모아 추리해 나가는 과정은 추리소설을 읽는 것만큼 흡입력 있고, 마침내 모든 퍼즐을 맞췄을 때 드러나는 결말은 따뜻하고 환상적인 반전으로 놀라움을 준다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(103,1121,'전문가가 꼼꼼히 감수한 과학 지식을 어린이들이 좋아하는 올컬러 만화로 재미나게 풀어낸 「초등과학학습만화 WHY」시리즈 『미래과학』. 로봇, 우주, 생명, 생활, 에너지, 정보 통신 등 과학 전반에 걸쳐 우리의 삶을 풍요롭게 해 줄 미래 과학 기술에 대해 소개한다. 재미난 이야기와 익살맞은 그림이 충실히 담겨있어 과학 지식과 조화를 잘 이루어 재미나면서도 깊이 있는 과학을 접할 수 있다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(104,1122,'고생물학에서 핵물리학까지, 끝없는 호기심을 가진 아이들에게 세상의 비밀을 알려 주는 어린이 교양서 「초등학생이 알아야 할 100가지」 시리즈. 『초등학생이 알아야 할 과학 100가지』는 우주론부터 생물학, 화학, 물리학, 지구 과학, 항공 역학 등 과학 분야에서 재미있는 사실들을 만날 수 있어요. 모든 생명의 조상, 대기의 구조, 칼 린네의 학명 체계와 같은 중요 개념부터 지구의 속도, 과학자들이 외계 생명체를 찾는 영역, 번개의 온도, 세상에서 가장 깊은 바다, 특별한 능력이 있는 생물들, 인간 컴퓨터, 살아 있는 나무의 반전 등 분야를 넘나드는 최신 과학 정보까지 두루 다루고 있어요. 새로운 지식을 배우고 지적 호기심을 키워 나갈 수 있는 매력적인 과학 이야기가 가득하지요.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(105,1123,'로봇에 담긴 과학 원리를 이해하고 나만의 로봇을 만들어 보자! 로봇에 대한 모든 것, 『내일은 로봇왕』 제3권. 본격 대결 과학로봇 만화 은 축구로봇과 배틀로봇이라는 흥미로운 소재를 중심으로, 로봇의 역사와 숨겨진 과학 원리를 쉽고 재미있게 전달하는 로봇 학습만화입니다. 매력적인 주인공들이 이끌어 나가는 재미있는 이야기와 정보 페이지의 풍부한 학습 내용을 통해 최초의 컴퓨터는 어떤 모습이었는지, 프로그래밍 언어가 어떻게 발달해 왔는지 알 수 있고, 컴퓨터 프로그래밍의 기초가 되는 이진법에 대한 내용도 배울 수 있습니다.\n\n그리고 이진법을 개발한 수학자 라이프니츠, 언플러그드 컴퓨팅 활동 등에 대한 정보도 얻을 수 있습니다. 또 로봇을 직접 만들며 살펴볼 수 있는 ‘로봇 키트’를 통해 로봇에 담긴 과학 원리를 좀 더 쉽게 이해할 수 있고, 키트 가이드에 소개된 모습과 다른 형태로 새롭게 조립하며 창의력를 키울 수 있습니다. 의 재미있는 이야기와 로봇 키트는 로봇을 좋아하는 아이들은 물론, 어렵게 느끼는 아이들도 로봇에 흥미를 붙이고 쉽게 이해할 수 있도록 돕는 훌륭한 친구가 될 것입니다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(106,1124,'아이들에게 과학에 대한 놀라운 지식을 전해주는 「초등과학학습만화」 제4권 『WHY? 컴퓨터』. 아이들이 자신과 또래인 \'꼼지\'와 \'엄지\'가 컴퓨터 전문가 \'실리\'와 컴퓨터 프로그램 \'디버거\'와 함께하는 신나는 모험을 통해 과학에 대한 놀라운 지식을 재미있게 배워나가도록 구성한 과학학습만화다. 가정, 방송국, 공장, 우주선, 병원 등에서 중요한 역할을 하는 \'컴퓨터\'에 대해 다룬다. 컴퓨터에 대해 올바르게 이해하여 미래 첨단 시대를 준비하도록 이끌고 있다. 생생한 사진도 곁들여 이해를 도와준다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(107,1125,'아이들에게 과학에 대한 놀라운 지식을 전해주는 「초등과학학습만화」 제36권 『WHY? 뇌』. 아이들이 자신과 또래인 \'꼼지\'와 \'엄지\'가 영리하고 똑똑한 휴머노이드 \'해마\'와 함께하는 신나는 모험을 통해 과학에 대한 놀라운 지식을 재미있게 배워나가도록 구성한 과학학습만화다. 우리가 자고 있을 때조차도 쉬지 않고 일하는 \'뇌\'에 대해 탐구한다. 뇌의 구조와 역할부터 뇌에 좋은 음식까지 뇌에 대한 모든 것을 담아냈다. 생생한 사진도 곁들여 이해를 도와준다.',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(108,1126,'마녀 위니가 나오는 시리즈 동화 \'마녀 위니의 새 컴퓨터\'편. 마녀 위니에게 새 컴퓨터를 샀다. 마법책이나 요술 지팡이가 없어도 마우스만 한 번 클릭하면 척척 원하는 대로 마법이 걸렸다. 위니는 잔뜩 신이 나서 마법책이랑 요술 지팡이를 내다버렸지만 한밤 중에 마우스에게 심술이난 고양이 윌버가 마우스를 마구 굴리는 바람에 사고가 생겼다. 위니가 아침에 일어났을 때는 컴퓨터와 윌버가 사라져 버린 것이다. 위니는 마법책이랑 요술 지팡이도 없는데 이 사고를 해결할 수 있을까?',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(109,1127,'공부의 흥미와 재미를 경험하게 해주는 수학소설『수학특성화중학교』제1권. 수학 영재 집합소 수학특성화중학교에 첫 신입생들이 입학했다. 아이들은 매일같이 던져지는 수학 문제와 씨름하던 중 비밀조직 ‘제로’의 존재를 알게 된다. 제로에 다가가면 갈수록 그 안에 숨은 거대한 비밀이 드러나는데….',NULL,NULL,NULL,'2026-07-13 10:34:05',NULL),(110,1128,'『김상욱의 양자 공부』 저자가 《과학동아》에서 연재해 과학 독자들의 사랑을 한 몸에 받은 《양자 역학 좀 아는 척!》의 에피소드들을 취합하고 발전시켜 만든 역작이다. 어려운 개념을 정확한 정의와 유머러스한 비유를 통해 차근차근 이해시켜 나가는 방식으로 대중으로부터 압도적 신뢰를 받아 온 저자의 개성을 극적으로 살린 책이다. 또한 흥미와 호기심을 자극하는 가상 드라마 「양자 역학의 하루」를 통해 양자 역학의 진입 장벽을 과감히 허물었다. 100년에 걸친, 이해 불가능한 현상들을 설명하려는 물리학자들의 흥미진진한 이야기를 따라가다 보면 자연스레 양자적 사고 능력을 갖출 수 있다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(111,1129,'코딩맨과 함께하는 흥미로운 코딩 학습!\n\n코딩이 반드시 필요한 세상 속에서 하루가 다르게 실력을 키워 가는 이 시대의 영웅과 같은 인물 유강민을 주인공으로 한 학습만화 『코딩맨』 제1권 《버그킹덤의 차원 침략》. 만화를 통해 코딩에 대한 흥미, 코딩 공부를 위한 동기와 의욕을 만들어가고 코딩이 재미있는 것이고, 왜 반드시 배워야 하는 분야인지 알게 해준다.\n\n인간이 살고 있는 차원 너머의 차원. 그곳은 인간의 감정 따윈 찾아볼 수 없는, 프로그램으로 코딩된 종족들의 공간이 존재한다. 한 무리가 인간 세계를 버그로 감염시키려 한다. 버그킹덤! 그들을 막기 위해 분투 중인 디버깅! 그리고 갑자기 알 수 없는 능력이 생겨 버린 평범한 초등학생 유강민. 이후 강민의 주변에서 자꾸만 이상한 일들이 벌어지는데…….',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(112,1130,'\'특별한 기회\'와 \'역사문화적 유산\'의 두 측면에서 상위 1%의 성공비결을 분석!\n\n이 책은 글로벌 베스트셀러인『티핑포인트』『블링크』의 저자 말콤 글래드웰의 저서이다. 컴퓨터 황제 빌 게이츠, 프로그래밍 귀재 빌 조이, 록의 전설 비틀스 등 이 시대 아웃라이어들이 성공할 수 있었던 결정적인 비밀을 파헤친다. 그 동안 우리가 보지 못했던 성공의 색다른 측면을 제시한다.\n\n제목은 \'아웃라이어\'는 본체에서 분리되거나 따로 분류되어 있는 물건, 또는 표본 중 다른 대상들과 확연히 구분되는 통계적 관측치를 의미한다. 이 책에서는 보통 사람들의 범주를 뛰어넘은 특별한 사람, 즉 천재를 뜻한다. 본문은 21세기 성공의 새로운 모델로 이 아웃라이어를 주목하며 성공과 부의 비밀을 밝힌다.\n\n1부에서는 성공한 사람들의 흔해 빠진 최루성 신파극이나 평범한 사람이 감히 접근접할 수 없는 영웅들의 무용담 대신, 성공하는 사람들이 누렸던 행운과 그들이 태어난 시대적 배경 및 세대 등의 요인을 객관적으로 살펴본다. 2부에서는 개인은 결국 사회라는 문화적 테두리 안에서 성공할 수 밖에 없다는 것을 강조한다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(113,1131,'와이즈만 미래과학 3권. 뇌 과학의 기초를 초등학생도 쉽게 이해할 수 있도록 서술한 국내 최초의 초등용 해설서이다. 뇌 과학의 역사와 미래 전망에 이르기까지 인간의 뇌를 둘러싼 다양한 궁금증들을 쉽고 재미있게 소개했다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(114,1132,'기존의 ｀신기한 스쿨버스｀ 시리즈가 다소 어렵게 느껴졌을 초등 저학년생이나 취학전 아이들을 대상으로 기획된 시리즈. 또한 이 책은 TV에서 방영되었던 ｀신기한 스쿨버스｀ 프로그램을 간추려 책으로 만든 것이어서 내용은 더욱 쉬워지고 글의 이야기적 요소가 더 증가되었다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(115,1133,'이름도 몰랐던 과학자들이 친근해지는 진짜 과학-만화책!\n\n재미와 과학, 둘 다 놓치지 않으려는 노력의 산물『야밤의 공대생 만화』. 자라나는 새싹들에게 ‘노잼’ 과학만화를 읽혀서 어린 시절부터 트라우마를 겪고 ‘과알못’의 길을 걷지 않도록, 서울대 공대생이 직접 구상하고 글과 그림까지 혼자 그린 책이다. 복학 직전에 서울대 커뮤니티 사이트 ‘스누라이프’에 트랜지스터의 발명자에 대한 에피소드를 올린 것을 시작으로 페이스북과 카카오플러스친구, 딴지일보에서 1년 반 동안 연재하며 인기 만화로 자리 잡았고, 책으로 읽고 싶다는 독자들의 바람에 힘입어 연재분 29편을 수정·보완하여 출간했다.\n\n유명 과학자부터 조금은 생소한 과학자까지, 고등학교 때 배우는 미적분부터 알파고까지, 과학사의 중요한 인물과 사건을 시시콜콜 나열하거나 무리해서 어려운 과학을 설명하려 하기보다, 원리와 의미를 짧고 깔끔하게 설명하면서도 재미있게 읽고 핵심을 이해하며 넘어갈 수 있도록 구성했다. 인터넷 유머와 센스 넘치는 패러디로 전달하면서 이름만 알았던 과학자들의 새로운 인간미를 접할 수 있게 했다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(116,1134,'어려운 가정 상황 때문에 마음이 지쳐있는 순남이는 교실에서도 아무도 말을 걸어주지 않는 외톨이다. 컴퓨터 수업 시간에 메일보내기 실습을 하면서, 읽고 있던 동화책의 작가에게 이메일을 보내게 된다. 그리고 얼마 후, 작가 선생님은 순남이에게 답신을 보내준다.\n\n\n\n작가 선생님과 메일을 쓰면서 순남이는 자기 이름 대신 반에서 가장 똑똑하고 예쁜 혜민이의 이름을 쓴다. 순남이는 혜민이 이름으로 메일을 쓰면서 대리 만족을 느끼기도 하고, 선생님의 따뜻한 답신에 용기를 얻는다. 혜민이는 우연히 책을 매개로 순남이와 친구가 된다.\n\n\n\n작가 선생님과 편지를 주고받으면서 순남이에게는 좋은 일이 많이 생긴다. 혜민이와 친구가 되고, 자신의 꿈을 이룰 수 있는 방법도 찾게 된다. 그렇지만, 거짓은 언제나 탄로가 난다. 작가 선생님은 순남이에게 책을 보내려다가 순남이가 이름을 속이고 메일을 보냈다는 사실을 알게 된다.\n\n\n\n작은 거짓말에서 시작한 일들이 일으키는 사건과 그 사이에서 느껴지는 긴장감, 섬세하게 표현된 아이의 내면이 공감을 불러 일으킨다. 타인과 소통하면서, 자기 자신을 있는 그대로 인정하게 되고, 좀더 나은 미래를 향해 나아갈 용기를 얻는 성장 이야기다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(117,1135,'세계 석학들의 대담한 고찰을 통해 인류의 미래를 위한 최선을 길을 찾다!\n\n20세기 부와 평화를 담보했던 기성 체제는 빠른 속도로 무너져 내리고, 인류 문명이 질적으로 완전히 다른 국면에 진입할 순간이 머지않아 보이는 지금, 몇몇 숫자와 조어로 포장된 단기 예측보다 변화의 방향과 강도를 입체적으로 분석해 미래를 적극적으로 사유할 수 있게 돕는 지혜가 절실하게 필요하다. 우리 문명에 다가올 지각변동을 날카롭게 통찰하는 세계 석학 8명과의 대담을 엮은 『초예측』은 전환의 길목에서 결정된 미래를 수용하는 대신 더 나은 미래를 만들어나가고자 하는 모든 이들에게 훌륭한 길잡이가 되어준다.\n\n《사피엔스》의 저자로 인류의 앞날을 고민하는 역사학자인 유발 하라리와 퓰리처상 수상작 《총, 균, 쇠》의 저자이며 세계적 문명 연구가인 재레드 다이아몬드를 비롯해 인공지능 연구가 닉 보스트롬, 인재론 권위자 린다 그래튼, 경제학 대가 다니엘 코엔, 노동법 전문가 조앤 윌리엄스, 인종사학자 넬 페인터, 전 미 국방부 장관 윌리엄 페리가 놈 촘스키, 마이클 샌델, 짐 로저스 등 세계 주요 인사들과 단독 인터뷰를 해온 경험 풍부한 국제 저널리스트 오노 가즈모토의 진행으로 미래에 대한 생각을 독자들과 나눈다.\n\n최악의 경우를 포함해 모든 가능성을 제시하는 것이 학자로서의 소명이라고 밝히는 유발 하라리는 과학기술의 급속한 발전으로 대부분의 인간이 경제적, 정치적 가치를 잃고 ‘무용 계급(useless class)’으로 전락할 수 있다고 경고하면서 위기가 현실이 되기 전에 지금 바로 움직일 것을 당부한다. 재레드 다이아몬드는 앞으로 우리에게 닥칠 세 가지 위협으로 신종 감염병, 테러리즘, 타국으로의 이주를 꼽으며 그 원인이 되는 국가 간 격차를 줄이기 위해 국제적 노력이 이뤄져야 한다고 주장하는 등 변화에 휩쓸리지 않고 위험에 맞서 최선의 길을 찾을 수 있는 힘을 선사하는 석학들의 통찰을 만나볼 수 있다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(118,1136,'상상하는 모든 것이 현실이 되는, 게임에 관한 모든 것!\n\n즐거움을 추구하는 인간의 본능 때문에 인류의 시작부터 행해져 왔던 게임은 컴퓨터 기술이 발전하면서 더 재미있어지고, 더 대중화되었다. 심지어는 직접 하지 않고 마치 스포츠를 관람하듯 다른 이들이 게임하는 모습을 보는 것을 즐기는 사람도 늘어났다.\n하지만 현실에서는 할 수 없었던 것들을 게임 속 캐릭터가 대신하며 대리 만족을 느낄 수 있기 때문에 게임에 한번 중독되면 빠져나오기 어려울 수도 있다. 그렇게 되면 일상생활뿐 아니라 건강에도 악영향을 미칠 수 있으며, 가족, 친구 등 현실의 소중한 것을 잃을 수도 있다.\n2019년 세계 보건 기구(WHO)는 게임 이용 장애(게임 중독)를 질병으로 규정하며, ‘질병 코드’를 부여했다. 하지만 코로나 19로 위기를 겪고 있는 2020년, WHO의 사무총장은 사회적 거리두기를 강조하며, 집에서 즐길 수 있는 여가 혹은 치유의 목적으로 게임을 권장하는 상반된 모습을 보이기도 했다. 이처럼 게임에 대한 논란은 여전히 뜨겁다. \n게임은 이제 단순한 취미를 넘어 교육, 의료, 생산 등 다양한 산업과 융합하여 미래를 주도하는 차세대 산업으로 주목받고 있기에, 컴퓨터 게임에 대한 다양한 정보를 찾고 그 원리를 이해하는 것은 다가올 미래를 준비하는 좋은 공부가 될 것이다. 또한 게임 자체가 무조건 나쁜 것만은 아니므로 적절한 기준을 세우고, 게임 하는 시간을 지킨다면 삶의 적절한 활력소가 될 것이다.\n〈Why? 게임 과학〉에는 온라인 컴퓨터 게임을 사랑하는 소녀 엄지와 텃밭 가꾸는 것을 좋아하는 소년 꼼지가 등장한다. 게임을 탐탁잖게 여기는 꼼지에게 엄지는 게임의 매력에 대해 알려 준다. 최초의 게임 스페이스 워부터 최신 VR 게임까지 게임의 역사에 대해서 훑어보고, 게임이 만들어지는 과정, 원리에 대해 알고 나만의 게임까지 직접 만들며 게임에 점점 흥미를 갖게 되는 꼼지! 과연 꼼지의 마음은 어떻게 바뀌었을까?',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(119,1137,'목 디스크는 예방하고, 거북목은 수술 없이 치료한다!\n\n《백년 허리》로 요통과 허리 디스크의 비밀을 밝혀내고, 허리 망치는 운동과 자세 대신 허리의 보증 기간을 100년으로 늘리는 운동과 자세를 제안해 온 정선근 서울대 의대 재활 의학과 주임 교수가 이번에는 목 디스크 해방에 도전한다. 『백년 목』에서 저자는 전작에서 소개한 허리와 목 디스크에 대한 의 ㆍ 과학 논의를 심화 발전시켜 소개한다. \n\n목과 허리 디스크가 발생하는 원인, 목과 허리 디스크 손상으로 발생하는 방사통과 연관통의 메커니즘, 목 디스크와 허리 디스크 구조의 해부학적, 재활 의학적, 진화적 연관성, 목과 허리 디스크의 문제를 해결하기 위해 노력해 온 100여 년에 걸친 의학계의 논쟁과 연구 성과 등을 일반 독자, 일반 환자의 눈높이에서 알기 쉽게, 그리고 현장감 넘치는 임상 사례와 함께 들려준다.\n\n1, 2, 3장에서는 목 디스크가 손상되는 이유를 설명한다. 하루 종일 노트북 컴퓨터와 씨름하는 전문직, 사무직 종사자들의 고된 일과, 스트레스와 우울증, 수면, 운전, 텔레비전 시청 같은 일상 생활, 교통 사고나 스포츠 손상 등이 어떻게 목 디스크를 파괴하는지를 정리하고 있다. 4, 5, 6장은 목 디스크 병의 증상에 대한 내용을 담았다. \n\n7, 8, 9장은 목 디스크 병에 대한 진단과 치료 방침의 결정이 결코 간단치 않음을 설명하면서 개념 있는 전문가와 개념 있는 환자가 만날 때 최선의 결과가 보장된다고 이야기하고, 아무런 효과도 없는, 해로울 수도 있는, 턱없이 비싼, 각종 혹세무민 치료법들이 이 시대를 풍미하는 복잡한 이유도 짚어 본다. 10, 11장은 목 디스크 병을 예방하고 치료하기 위해 독자들이 스스로 해야 할 것들을 알려 준다. 특히 좋은 운동으로 잘못 알려져 있는 목 디스크 운동들의 오류를 지적하고 진짜로 좋은 운동은 어떤 것인지 명쾌하게 소개하고 있다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(120,1138,'사회 기여를 위한 그의 도전은 계속된다!\n\n오늘날의 대한민국을 비롯한 이 시대를 이끄는 ‘현대 대표 인물’의 이야기로 구성되어 있는『Who? Special』시리즈 안철수 편. 너무 먼 시대에서 활동하여 공감하기 어려운 기존 위인전의 인물이 아닌, 현재 사회 전반에 커다란 영향력을 미치고 있는 인물의 삶을 보여줌으로써 보다 실질적인 \'롤모델\'을 구현했습니다. 화려하게만 보였던 성공 뒤에 숨겨져 있는 고통은 물론, 한때는 누구보다 나약했고 실패에 절망했던 이야기가 그려집니다.\n\n컴퓨터 백신 개발자에서 국내 최고 벤처 기업 경영자, 대한민국을 이끄는 정치 리더까지! 눈앞에 보이는 안정된 삶을 뒤로하고 도전에 도전을 거듭한 안철수를 만나보세요. 책 속 부록인 ‘통합지식 플러스’ 코너에서는 의사에서 프로그래머, 또 기업인을 거쳐 국회 의원으로 거듭난 안철수의 노력에 대해 자세히 확인할 수 있습니다. 또한 권말 ‘어린이 진로 탐색’에서는 ‘국회 의원’이라는 직업을 중심으로 진로를 탐색합니다.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(121,1139,'자신의 부정적 감정을 극복하도록 안내해주는 책!\n\n아이들에게 오싹한 즐거움을 선사해주는 본격 호러 동화 『구스범스』 제5권 《악령을 부르는 머리》. 영어로 ‘소름’이라는 뜻을 지난 구스범스 시리즈는 1992년 미국에서 첫 출간된 이래 20여 년 동안 꾸준한 사랑을 받아, 어린이가 뽑은 가장 재미있는 책에 수여하는 ‘니켈로디온 상’을 3번이나 받은 작품이다. 악마 같은 저주인형, 죽지 않는 악마가면, 반인반수 늑대인간 등 아이들에게 친숙한 초자연적 존재들을 통해 공포를 조성한다. 어린이가 느낄 수 있는 낯선 것에 대한 스트레스, 분리 불안, 형제자매나 친구 사이의 경쟁 등을 어린이의 시각에서 흥미롭게 다뤘다. \n\n이모의 초대로 떠난 정글의 섬에서 마크는 청천벽력 같은 사실을 알게 된다. 이모는 행방불명되었고, 마크를 반겨 주었던 정글 섬 과학자는 잔인한 악당이었던 것이다. 설상가상, 악당은 마크에게 고대 정글의 마법을 내놓으라고 협박한다. 마크는 주문을 외우면 엄청난 힘이 생긴다는 정글 마법의 주문을 알고 있는 특별한 아이라는 사실을 처음으로 알게 된다. 칠흑 같은 정글의 밤, 기괴한 새의 울음소리, 끈질기게 뒤를 밟는 추격자가 마크는 두렵기만 하는데….',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(122,1140,'생각하고, 말하고, 스스로 배우는 기계\n인공 지능과 함께 살기 해설서\n\n“인공 지능의 역사와 미래 전망에 이르기까지 다양한 궁금증에 대한 해답”\n\n기계가 사람만큼 똑똑해질 수 있을까?\n지능이 있고, 생각하고, 의식이 있는 인공 지능이 올까?\n강한 인공 지능이 언제 올까?',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(123,1141,'퀴즈로 배우는 미래과학 이야기『퀴즈! 과학상식: 미래과학』. 투명망토가 실제로 있는지, 가짜 태양을 만들 수 있는지, 스스로 흠집을 치료하는 자동차가 있는지 정말 궁금하지요? 말썽꾸러기 주인공들이 벌이는 엉뚱한 사건을 통해 미래과학에 대한 여러분의 궁금증을 확~ 풀어 보세요.',NULL,NULL,NULL,'2026-07-13 10:34:06',NULL),(124,1142,'상상 그 이상의 혁신을 이끌 IT 신기술\n\n암호 화폐, 그리고 블록체인. 이름부터 어려운 이 두 가지가 대체 뭔데 전 세계가 들썩이고 있는 걸까? 암호 화폐는 말 그대로 암호화 기술이 적용된 전자 화폐다. 그리고 블록체인은 암호 화폐의 기본 원리라고 할 수 있다. 거래 내역이 담긴 하나하나의 블록을 쇠사슬 모양으로 연결하여 이를 복제해 수많은 컴퓨터에 저장하는 방식인데, 이는 지금까지 보지 못한 신개념 시스템이다. 거래에 참여하는 모든 사람들이 이를 검증하기 때문에 위조나 변조가 이루어지기 힘들고 보안이 매우 뛰어나다. \n\n〈Why? 암호 화폐와 블록체인〉에서는 대표적인 암호 화폐인 비트코인을 개발한 사토시 나카모토를 컴퓨터 동아리 멤버로 설정하고 개발자들의 의도와 다르게 변해 가는 암호 화폐에 위기감을 느껴 앞으로의 행보를 결정하기 위해 아이들의 마음을 들여다본다는 내용을 담고 있다. 이 과정에서 암호 화폐와 블록체인에 관한 보다 자세한 내용은 물론, 올바른 활용 방안과 미래 네트워크 기술과의 융합을 예측해 볼 수도 있다.\n\n4차 산업 혁명 시대를 이끄는 다양한 네트워크 기술의 발달과 블록체인의 뛰어난 보안성이 더해진다면 상상 이상의 눈부신 변화가 일어날 것이다. 이 책을 통해 새로운 기술에 한발 다가가 무한한 가능성을 지닌 미래의 주역이 되어 보는 건 어떨까?',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(125,1143,'전문가가 꼼꼼히 감수한 과학 지식을 어린이들이 좋아하는 올컬러 만화로 재미나게 풀어낸 「초등과학학습만화 WHY」시리즈 『정보통신』. 상품의 가치를 갖고 있는 정보를 시작으로, 우리 실생활을 보다 편리하게 도와주는 정보통신의 기술을 알려준다. 재미난 이야기와 익살맞은 그림이 충실히 담겨있어 과학 지식과 조화를 잘 이루어 재미나면서도 깊이 있는 과학을 접할 수 있다.',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(126,1144,'국내 최대 과학관의 최고 과학자들이 \n들려주는 진짜 과학 이야기\n\n『과학이 톡톡 쌓이다! 사이다 2 : 인공지능』\n4차 산업혁명 시대가 도래했고, 이제는 인공지능이 우리의 일상을 크게 차지하고 있다. 하지만 여전히 인공지능이 무엇이고, 어떤 역할을 하는지 이해하기 어려워하는 어린이들이 많다. 이를 위해 인공지능에 대해서 꼭 알아야 할 내용을 남녀어린이와 인공지능이 친구처럼 편안하게 대화하는 형식에 담아 구성하였다. 인간의 뇌에서부터 딥 러닝, 약인공지능과 강인공지능, 자율 주행 자동차와 빅데이터까지 인공지능과 관련된 모든 내용에 대해 묻고 답한다. 또 추상적이고 어려운 개념들을 그림으로 표현해 어린이 독자들이 쉽게 이해할 수 있도록 구성하였다.',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(127,1145,'\'스마트\' 시대, 우리는 더 똑똑해지고 있습니까?\n\n인터넷, 정보기술, 스마트 기기의 발달은 우리의 정보와 지식을 습득하는 방식에 거대한 변화를 불러일으켰다. 책 한 권을 다 읽고 밑줄을 그으며 지식을 얻기보다 인터넷 검색을 통해 단 몇 분 만에 손쉽게 정보를 습득할 수 있는 세상이 열린 것이다. 우리 주변에는 놀라울 정도로 많은 양의 정보들이 차고 넘치며, 사람들은 걸어 다니면서도 정보를 찾아낸다. 그렇다면 우리는 정말 더 스마트해졌을까? IT 전문가이자 저명한 칼럼리스트인 저자 니콜라스 카가『생각하지 않는 사람들』을 통해 디지털 기기에 종속된 이후 우리의 사고하는 방식은 어떻게 변화하고 있는지, 글을 쓰는 방식과 읽는 방식은 어떻게 변화하고 있는지를 밝힌다.',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(128,1146,'쉽고 재밌는 『분수와 소수』는 초등 학습과 연계된 컴퓨터와 코딩부터 수학, 과학과 예술, 스포츠, 역사까지 다양한 주제를 담고 있습니다. 열면 또 열리는 열면 또 열리는 플랩 속 플랩은 ‘호기심’을 돋우고, ‘집중력’을 기르고, 점점 더 깊은 ‘탐구심’으로 이어질 수 있도록 도와줍니다.어려운 지식책은 이제 그만! 기본 개념부터 원리를 이해하는 단계까지, 보다 깊이 있는 학습 효과를 함께 해보세요.',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(129,1147,'『휴대폰 전쟁』는 휴대폰 중독을 다룬 청소년 소설이다. ‘접속’ 상태에서만 자신의 존재감을 느끼는 중학생 다리아를 통해 휴대폰 중독의 위험성을 일깨워 준다. 요즘 청소년들의 일상을 섬세하면서도 매우 사실적으로 그리고 있어 휴대폰 중독의 현주소를 생생하게 보여주고 있으며 휴대폰 사용 습관을 되돌아 보게 한다.',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(130,1148,'코딩과학동화 〈팜〉 두 번째 이야기!\n이번엔 하늘 농장이다!\n\n초등학생에게 코딩과 과학을 쉽게 알려주기 위해 기획된 코딩과학동화 《팜》 시리즈의 두 번째 이야기 ‘하늘 농장’ 편이 출간되었습니다. \n\n1권 지하 농장에 이어서, 2권에서는 하늘 농장이라는 전혀 다른 공간으로 이동합니다. 쌍둥이 형제 주니와 거니는 하늘 음악당, 태양 가림막 우산, 액괴 놀이방, 하늘 배 주차타워 등 하늘 농장에 필요한 장소와 발명품들을 만들면서 하늘 농장을 가꾸기 시작합니다. 그렇게 평화롭게(!) 지내려던 찰나 마른 하늘의 날벼락, 천둥 번개가 들이닥쳐 하늘 농장이 돌덩이처럼 굳어버리는데요.\n\n주니와 거니가 이 위기를 극복하는 과정에서 아이들은 자연스럽게 코딩 개념을 접하게 됩니다. 특별히 2권에서는 ‘네트워크, 센서, 컴퓨터 바이러스와 백신, 배열과 리스트, 이진수’ 같은 조금 더 확장된 컴퓨터과학 개념을 담았습니다.\n\n한층 더 재밌는 줄거리와 상상력 넘치는 그림, 덤으로 코딩에 대한 지식까지 얻을 수 있는 《팜》 두 번째 이야기를 지금 만나 보세요!\n\n[줄거리]\n지하 농장에서 거대 나무줄기를 타고 하늘로 올라 온 거니와 주니. 거대 나무줄기 끝에는 아무도 살지 않는 하늘 정원이 있다. 하늘 정원에 도착한 주니는 하늘 농장으로 만들어 살기를 원한다. 하지만 시작도 하기 전에 구름 폭탄을 발로 밟아 위기를 맞이한다. 우여곡절 끝에 구름 폭탄을 해체하고 본격적으로 하늘 농장을 가꾸기 시작한 주니와 거니. 뜨거운 태양 빛을 막는 자동 센서 태양 가림막을 시작으로 팝콘 게임방을 대신할 액괴 놀이방, 침입자를 감지하는 하늘 배, 기린의 하프 연주를 들을 수 있는 하늘 음악당 등 하늘 농장은 점차 제 모습을 갖춰간다. 그렇게 평화롭던 하늘 농장에 갑자기 천둥 번개가 내리치더니 번개가 닿은 곳은 모두 돌덩이로 변해 가는데...',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(131,1149,'컴퓨터 속 매트릭 랜드에 살고 있는 인기 캐릭터 도티와 잠뜰! 인기 투표에서 꼴찌를 한 도티가 영원히 사라질 위기에 처합니다. 도티가 살아남을 방법은 딱 하나! 현실 세계의 인간 팬을 데리고 코딩 하트를 찾아내야 해요. 코딩 하트가 있는 곳으로 가기 위해 지도와 나침반을 찾는 과정에서 사사건건 훼방을 놓는 도티의 닮은꼴, 더티의 정체는 무엇일까요? 도티와 잠뜰은 무사히 코딩 하트를 찾을 수 있을까요? \n\n『도티&잠뜰 코딩』 방과후 학교 시리즈 《컴퓨터 편》은 2018년부터 소프트웨어 교육이 의무화되면서 어린이들이 꼭 알아야 할 컴퓨터와 코딩 학습 내용을 재미있게 담아냈습니다. 재미있는 스토리를 읽기만 해도 컴퓨터와 코딩의 기초를 확실히 이해할 수 있습니다. 도티, 잠뜰과 함께하는 흥미진진한 모험을 통해 컴퓨터 프로그래밍에 흥미를 느껴 보세요!',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(132,1150,'유아들을 위한 재미있는 과학 동화책. 우리는 신기한 스쿨 버스를 타고 사막 동물들을 구하러 갔어요. 그런데 사막에 도착하자마자 배고픈 길달리기새가 우리를 먹으려고 따라왔어요. 우리는 오늘 견학을 무사히 마칠 수 있을까요?.. 주인공 아이들의 신나는 사막 여행을 통해 사막 생물들이 살아가는 방법을... ',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL),(133,1151,'쉽고 재밌는 『컴퓨터와 코딩』은 초등 학습과 연계된 컴퓨터와 코딩부터 수학, 과학과 예술, 스포츠, 역사까지 다양한 주제를 담고 있습니다. 열면 또 열리는 열면 또 열리는 플랩 속 플랩은 ‘호기심’을 돋우고, ‘집중력’을 기르고, 점점 더 깊은 ‘탐구심’으로 이어질 수 있도록 도와줍니다.어려운 지식책은 이제 그만! 기본 개념부터 원리를 이해하는 단계까지, 보다 깊이 있는 학습 효과를 함께 해보세요.',NULL,NULL,NULL,'2026-07-13 10:35:45',NULL);
/*!40000 ALTER TABLE `book_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_isbn_temp`
--

DROP TABLE IF EXISTS `book_isbn_temp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_isbn_temp` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `isbn13` char(13) NOT NULL,
  `book_title` varchar(255) DEFAULT NULL,
  `publisher` varchar(100) DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `cover_image_url` varchar(500) DEFAULT NULL,
  `description` text,
  `pub_date` varchar(20) DEFAULT NULL,
  `aladin_category_id` varchar(20) DEFAULT NULL,
  `aladin_category_name` varchar(255) DEFAULT NULL,
  `naver_res_status` tinyint NOT NULL DEFAULT '0',
  `aladin_res_status` tinyint NOT NULL DEFAULT '0',
  `status_cd` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_isbn_temp_isbn13` (`isbn13`)
) ENGINE=InnoDB AUTO_INCREMENT=242 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_isbn_temp`
--

LOCK TABLES `book_isbn_temp` WRITE;
/*!40000 ALTER TABLE `book_isbn_temp` DISABLE KEYS */;
INSERT INTO `book_isbn_temp` VALUES (78,'9788936434120','소년이 온다 :한강 장편소설 ','창비','지은이: 한강','https://image.aladin.co.kr/product/4086/97/cover200/8936434128_2.jpg','말라파르테 문학상, 만해문학상 수상작 \n우리 시대의 소설 『소년이 온다』\n\n2014년 만해문학상, 2017년 이탈리아 말라파르테 문학상을 수상하고 전세계 20여개국에 번역 출간되며 세계를 사로잡은 우리 시대의 소설 『소년이 온다』.\n이 작품은 『채식주의자』로 인터내셔널 부커상을 수상한 한강 작가에게 “눈을 뗄 수 없는, 보편적이며 깊은 울림”(뉴욕타임즈), “역사와 인간의 본질을 다룬 충격적이고 도발적인 소설”(가디언), “한강을 뛰어넘은 한강의 소설”(문학평론가 신형철)이라는 찬사를 선사한 작품으로, 그간 많은 독자들에게 광주의 상처를 깨우치고 함께 아파하는 문학적인 헌사로 높은 관심과 찬사를 받아왔다. \n『소년이 온다』는 ‘상처의 구조에 대한 투시와 천착의 서사’를 통해 한강만이 풀어낼 수 있는 방식으로 1980년 5월을 새롭게 조명하며, 무고한 영혼들의 말을 대신 전하는 듯한 진심 어린 문장들로 5·18 이후를 살고 있는 우리에게 묵직한 질문을 던진다. \n이 작품은 가장 한국적인 서사로 세계를 사로잡은 한강 문학의 지향점을 보여준다. 인간의 잔혹함과 위대함을 동시에 증언하는 이 충일한 서사는 이렇듯 시공간의 한계를 넘어 인간 역사의 보편성을 보여주며 훼손되지 말아야 할 인간성을 절박하게 복원한다.','2014-05-19','50993','국내도서>소설/시/희곡>한국소설>2000년대 이후 한국소설',1,1,2,'2026-07-11 16:38:06','2026-07-11 16:40:58'),(189,'9791190305310','놓지 마 과학!','위즈덤하우스','글·그림: 신태훈,나승훈','https://image.aladin.co.kr/product/21502/77/cover200/k742636192_1.jpg','정신이는 최고의 과학 선생님!\n“아직도 〈놓지 마 과학!〉 안 보셨어요?”\n\n인기 절정의 웹툰 〈놓지 마 정신줄!〉의 작가들이 만든 과학 학습 만화 〈놓지 마 과학!〉 12권이 출간되었다. 1차분 1~5권과 2차분 6~10권에 이어 3차분 11~15권의 두 번째 권인 12권이 출간된 것이다. 〈놓지 마 과학!〉 시리즈는 1권이 2016년 8월에 출간된 이후, 매번 새 책이 나올 때마다 학습 만화 베스트셀러로 자리 잡으며 인기를 더하고 있다. 이 때문에 다른 책은 몰라도 이 책만은 꼭 본다는 충성도 높은 어린이 독자들의 수가 점점 늘어 가고 있으며, 새 책의 출간 시기를 묻는 문의 전화 또한 잇따른다고 한다. \n\n〈놓지 마 정신줄!〉은 2009년 8월 연재를 시작해 현재까지 11년째 이어 오며 조회 수 28억 뷰가 넘는 초인기 웹툰으로 성장했다. 또한 어린이 독자들의 사랑을 바탕으로 방송 애니메이션으로 만들어지기도 했다. 〈놓지 마 과학!〉 시리즈는 〈놓지 마 정신줄!〉의 캐릭터들이 등장하여 생활 속에서 생기는 과학적 질문들을 엉뚱하고도 기발한 전개를 통해 자연스럽게 풀어 주고 또한 예상을 빗나가는 결말로 웃음을 터뜨리게 만든다.','2019-11-08','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:18:04','2026-07-13 10:26:32'),(190,'9788930219556','Why? 소프트웨어와 코딩 ','예림당','글: 조영선 ;만화: 이영호','https://image.aladin.co.kr/product/7153/70/cover200/8930219551_1.jpg','논리력 사고력을 키우는 과학 만화!\n\n흥미진진한 모험 이야기에 소프트웨어와 코딩에 관한 내용을 재미있게 녹여낸 『Why? 소프트웨어와 코딩』. 앞으로 초등학교와 중학교에서 코딩 수업이 의무화된다. 하지만 C언어나 자바(Java)처럼 복잡한 명령어를 직접 입력해야 하는 프로그래밍 언어는 어린이들이 접근하기 어렵다. 그래서 등장한 것이 바로 스크래치나 엔트리 같은 블록형 프로그래밍 언어이다. 코딩은 단지 컴퓨터 프로그램을 움직이게 하는 데 그치는 것이 아니라 생각하는 방법을 찾고, 논리적인 사고력을 기르며, 문제를 해결하는 능력을 키워 준다. 이 책이 코딩에 대한 흥미를 유발하고 잠재력을 깨우는 좋은 자극이 될 것이다.','2015-12-15','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:18:04','2026-07-13 10:26:32'),(202,'9791168122369','놓지 마 과학!','위즈덤하우스','글·그림: 신태훈,나승훈','https://image.aladin.co.kr/product/29305/35/cover200/k992837600_1.jpg','100만 부를 돌파한 대한민국 대표 학습 만화! \nCSI 과학수사대를 방불케 하는 증거 찾기X최신 과학 정보X 권위 있는 과학 전문가 감수!\n\n2009~2019년 연재된 웹툰 〈놓지 마 정신줄!〉은 조회 수 28억 뷰가 넘는 초인기 웹툰으로 초등학생들의 절대적인 지지와 사랑을 받았다. 이후 애니메이션을 비롯해 웹드라마까지 만들어지며 나이와 연령을 불문하고 대중에게 사랑을 받는 콘텐츠로 자리매김했다. 이 인기에 힘입어 〈놓지 마 과학!〉 학습 만화 시리즈는 총 16권이 출간되었으며, ‘100만 부 판매를 돌파한 대한민국 대표 학습 만화의 결정판’이라 불리고 있다. \n드디어 《놓지 마 과학! 17권》이 발매되었다. 이번 권에서는 특별한 의뢰인 박순이가 사연을 들고 탐정사무소로 찾아온다. 박순이에게는 어떤 사연이 있는 걸까? 박순이네 아빠는 지구 온난화 위기를 전 세계에 알리겠다며 갑자기 투발루로 떠난다. 그런데 엉겁결에 모카노로 가는 비행기를 타게 되고, 거기다 내뿜내뿜 화산까지 분출해 강제로 모카노에서 지내게 된다. 박순이네 아빠는 모카노 투어 중 보물을 발견하게 되어 모카노 왕국 왕비로부터 초특급 보물 세트를 받는다. 이 보물을 둘러싸고 계속 이상한 사건 사고들이 벌어지는데……. \n《놓지 마 과학! 17권》에는 스릴감 넘치는 증거 찾기 과정이 담겨 있다. 또한 ‘정신이가 만난 과학자: 앙투안 로랑 라부아지에’ 편의 인터뷰가 실려 있으며, 하나의 과학 주제를 집중해서 샅샅이 살펴볼 수 있는 ‘정신이의 과학 X-Files’ 코너에서는 화산에 대한 과학 정보를 사진과 일러스트와 함께 풍부하게 구성해 독자들에게 볼거리를 제공한다. 본문에 담긴 학습 내용을 ‘정신이와 실력 뽐내기 퀴즈’ 코너를 통해 한 번 더 점검해 볼 수 있다. 특히 이번 권부터는 권위 있는 과학 전문가인 서울대학교 화학교육과 홍훈기 교수의 감수를 받아 책의 완성도를 더 높였다.','2022-05-06','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(203,'9788989646075','그림 도둑, 준모 ','낮은산','글: 오승희 ;그림: 최정인','https://image.aladin.co.kr/product/41/65/cover200/8989646073_2.jpg','평범한 아이 준모는 상을 한번 받아서 엄마를 기쁘게 하는 것이 소원이다. 엄마 역시 준모의 \'소질\'을 찾아 주지 못해 안달이다. 준모는 낙서처럼 그린 그림 때문에 \'소질\'을 발견(?)하게 되고, 그후 준모는 각종 그리기 대회를 겨냥해 맹연습에 들어간다. 그러나 상은 좀처럼 주워지지 않는다. 그리고 교내 불조심 그림 그리기 대회날, 준모는 작은 실수를 하게 된다. 하지만 그 실수가 큰 오해를 불러오는데...\n\n아이들을 무한 경쟁으로 내모는 어른들의 욕심과, 그속에서 상처받는 아이의 마음을 그리고 있는 이책은 우리 모두가 상을 받지 않아도 특별한 재능이 없어도 있는 그대로의 특별한 아이임을 알려주고 있다.','2003-05-30','48874','국내도서>어린이>동화/명작/고전>국내창작동화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(204,'9788984286269','할머니, 어디 가요? 밤 주우러 간다! :옥이네 가을 이야기 ','보리','조혜란 그리고 씀','https://image.aladin.co.kr/product/773/37/cover200/8984286265_1.jpg','옥이도 할머니도 바쁜 가을이 돌아왔어요\n\n봄, 여름, 가을, 겨울 자연 속에서 착하게, 신나게, 예쁘게 살아가는 「옥이네 이야기」 ‘가을’ 편 『할머니 어디 가요? 밤 주우러 간다!』. 끼룩끼룩 기러기 우는 가을날에 맛난 반찬 귀한 반찬 하러 들로 산으로 갯가로 뛰어다니는 일곱 살 옥이와 옥이 할머니의 이야기다. 할머니와 함께 덤불을 헤치고 밤을 주운 옥이는, 주운 밤으로 약밥, 송편 만들어 시장에 팔러 나간다. 그리고 할머니가 사주신 고운 한복 입고 할머니, 아빠, 엄마 모두 모여 차례도 지내고 맛있는 음식을 먹으며 추석을 보낸다. 자연 속에서 신나게 뛰어노는 옥이와 정겨운 시골 사람들의 이야기를 유쾌하고 발랄한 문장과 그림 속에 담아내 독자들을 이야기 속으로 빠져들게 한다.','2010-08-31','48874','국내도서>어린이>동화/명작/고전>국내창작동화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(205,'9788930206785','Why? 컴퓨터 바이러스 ','예림당','글: 조영선 ;만화: 이영호','https://image.aladin.co.kr/product/4458/69/cover200/8930206786_1.jpg','컴퓨터 바이러스로부터 우리의 안전을 지켜내자!\n\n아이들에게 과학에 대한 놀라운 지식을 전해주는 「초등과학학습만화」 제57권 『Why? 컴퓨터 바이러스』. 이 시리즈는 아이들이 딱딱하고 어려운 과학을 쉽고 재미있게 만나도록 엮었습니다. 만화 페이지와 정보 페이지가 어우러져 다양한 과학적 정보를 쉽고 일목요연하게 배워나갈 수 있습니다. 특히 모든 과학의 출발인 호기심과 궁금증을 시원하게 풀어줍니다.  \n\n인터넷을 사용하는 사람들이라면 한 번쯤 들어봤을 컴퓨터 바이러스에 대해 소개했습니다. 컴퓨터 바이러스의 원인이 무엇인지, 어떻게 감염되는지, 종류에 따라 어떤 증상이 나타나는 지 등을 알려줍니다. 더불어 다소 딱딱할 수 있는 IT용어와 개념들을 아이들이 쉽게 이해할 수 있도록 설명하고, 사이버 상에서 이루어지는 무분별한 행동들이 어떤 결과를 초래하는지에 대한 윤리적인 성찰도 담아냈습니다.','2014-08-15','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(206,'9788959779345','게임 없이 못 살아! ','좋은책신사고 좋은책어린이','안미연 글 ;이용규 그림','https://image.aladin.co.kr/product/773/82/cover200/8959779342_1.jpg','게임에게서 \'시간의 주인공\'의 자리를 되찾자!\n\n저학년 아이들을 위한 「좋은책어린이 저학년문고」 제25권 『게임 없이 못 살아』. 날마다 게임에 푹 빠져 사는 소년 \'시준이\'의 이야기를 담아낸 장편동화다. 엄마, 아빠와의 약속 시간도 지키지 않고 게임에만 매달리다가, 게임 세계로 들어가 이무기와 싸우게 된 시준이의 환상적 모험 속으로 아이들을 초대하고 있다. 게임만 하면 시간이 흘러가는지도 모르고 정신을 팔아버리는 아이들이 \'시간의 주인공\'이 되도록 이끈다. 게임에 끌려다니지 않고 스스로 조절하며 즐기도록 인도하는 것이다. 엄마, 아빠에게는 게임을 둘러싼 잔소리 전쟁을 끝내는 비결을 가르쳐준다.','2010-08-30','48874','국내도서>어린이>동화/명작/고전>국내창작동화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(207,'9788930231633','Why? SNS =소셜 네트워크 서비스 ','예림당','글: 조영선 ;그림: 이영호','https://image.aladin.co.kr/product/3642/25/cover200/8930231632_1.jpg','아이들의 교양 지식을 키우다!\n\n인문사회 교양만화 『Why SNS』. 국어ㆍ수학ㆍ사회ㆍ예체능 과목에 대한 학습 능력을 키우고 서술형 문제로 논리력과 창의력을 길러주는 책입니다. 각각의 주제는 초ㆍ중등학교 교과 과정과 연계하여 학습에 도움이 되도록 했습니다. 각 권마다 해당 분야의 전문학자ㆍ교수ㆍ연구원들의 세심한 감수로 내용의 정확성을 높였습니다. 어려운 전문 용어나 꼭 필요한 한자어는 풀이를 따로 두어 어린 독자들의 지적 호기심을 풀어줬습니다.\n\nSNS는 소셜 네트워크 서비스의 약자입니다. 인터넷 통신망을 통해 새로운 사람과 관계를 쌓거나 이미 알고 있던 사람과의 관계를 돈독하게 만들어줍니다. SNS가 어떻게 발전해 왔으며 우리에게 어떤 영향을 미치고 있는지 다뤘습니다. 더불어 앞으로 SNS를 어떻게 이용해야 행복한 미래를 만들 수 있는지 고민해볼 시간을 제공할 것입니다.','2014-01-10','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(208,'9788926386132','쿠키런 반짝 미래 과학 상식 :나의 미래 생활 신기한 미래 과학 25 ','서울문화사','글: 조주희 ;그림: 이태영','https://image.aladin.co.kr/product/8590/2/cover200/8926386139_1.jpg','웃으면서 배우는 과학 상식!\n\n일상생활에서 생겨나는 무궁무진한 호기심들을 과학적 원리를 이용해 간단명료하게 풀어내는 「쿠키런 펀펀 상식 시리즈」 제12권 『쿠키런 반짝미래 과학 상식』. 곧 다가올 미래 사회에서 우리가 어떤 환경에서 어떤 모습으로 살아가고 있을지를 쿠키들이 펼치는 25개의 에피소드를 통해 보여 줍니다. 그림을 통해 쉽고 재미있게 풀어 낸 과학 상식은 감수 선생님의 도움으로 더욱 정확도를 높였습니다. 귀여운 쿠키들의 황당하고 재미있는 에피소드를 읽다 보면 자연스럽게 학습 효과가 높아집니다.','2016-06-24','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(209,'9788930202794','Why? 과학사건 ','예림당','글: 박상욱 ;만화: 이태훈','https://image.aladin.co.kr/product/716/61/cover200/8930202799_1.jpg','전문가가 꼼꼼히 감수한 과학 지식을 어린이들이 좋아하는 올컬러 만화로 재미나게 풀어낸 「초등과학학습만화 WHY」시리즈 『과학사건』. 한국과학창의재단에서 발표한 \'인류 100대 과학 사건\' 중에서 밀접한 관련을 가진 사건을 모아, 만화와 사진, 세밀화로 재구성해 어린이들이 과학의 역사를 한눈에 볼 수 있도록 꾸몄다. 재미난 이야기와 익살맞은 그림이 충실히 담겨있어 과학 지식과 조화를 잘 이루어 재미나면서도 깊이 있는 과학을 접할 수 있다.','2008-12-31','49224','국내도서>어린이>초등 전학년>학습만화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(210,'9788949162119','사라진 물건의 비밀 ','비룡소','이분희 글 ;이덕화 그림','https://image.aladin.co.kr/product/28343/5/cover200/8949162113_1.jpg','이건 잃어버린 게 아니라 사라진 거야! \n툭하면 잃어버리기 잘하는 기찬이의 자기 물건 찾기 대작전\n\n국내 20만 부 판매 『한밤중 달빛 식당』 이분희 작가의 신작 『사라진 물건의 비밀』이 출간되었다. 이분희 작가는 비룡소 문학상과 황금도깨비상을 같은 해에 수상하고 수상작 모두 출간 이후 베스트셀러로 올라 신인 작가로서 단번에 뜨거운 주목을 받았다. 『사라진 물건의 비밀』은 이분희 작가가 오랜만에 독자들에게 선보이는 작품으로 작가 특유의 글맛과 입담이 살아 있는 따뜻한 판타지 동화다. \n\n자기 물건을 잘 간수하지 못해 엄마에게 늘 혼나는 기찬이는 어느 날 이번만은 자기가 잃어버린 게 아니라 물건이 사라지고 있다고 의심하며 그 사라진 물건들을 찾아 나서는 모험을 담았다. 물건을 하나 잃어버릴 때마다 일주일간 컴퓨터 사용 금지라는 무시무시한 ‘엄마법’이 생기자마자 벌어진 일로, 기찬이는 사라진 물건에 대한 단서들을 모으며 범인에 대한 윤곽을 잡아나간다. 기찬이가 단서를 하나하나 모아 추리해 나가는 과정은 추리소설을 읽는 것만큼 흡입력 있고, 마침내 모든 퍼즐을 맞췄을 때 드러나는 결말은 따뜻하고 환상적인 반전으로 놀라움을 준다.','2021-11-15','48874','국내도서>어린이>동화/명작/고전>국내창작동화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(211,'9788930202718','Why? 미래과학 ','예림당','글: 조영선 ;만화: 이영호','https://image.aladin.co.kr/product/716/60/cover200/8930202713_1.jpg','전문가가 꼼꼼히 감수한 과학 지식을 어린이들이 좋아하는 올컬러 만화로 재미나게 풀어낸 「초등과학학습만화 WHY」시리즈 『미래과학』. 로봇, 우주, 생명, 생활, 에너지, 정보 통신 등 과학 전반에 걸쳐 우리의 삶을 풍요롭게 해 줄 미래 과학 기술에 대해 소개한다. 재미난 이야기와 익살맞은 그림이 충실히 담겨있어 과학 지식과 조화를 잘 이루어 재미나면서도 깊이 있는 과학을 접할 수 있다.','2008-07-21','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(212,'9791186872802','(초등학생이 알아야 할) 과학 100가지 ','Usborne Korea','알렉스 프리스,최새미 옮김','https://image.aladin.co.kr/product/11256/10/cover200/k642531791_1.jpg','고생물학에서 핵물리학까지, 끝없는 호기심을 가진 아이들에게 세상의 비밀을 알려 주는 어린이 교양서 「초등학생이 알아야 할 100가지」 시리즈. 『초등학생이 알아야 할 과학 100가지』는 우주론부터 생물학, 화학, 물리학, 지구 과학, 항공 역학 등 과학 분야에서 재미있는 사실들을 만날 수 있어요. 모든 생명의 조상, 대기의 구조, 칼 린네의 학명 체계와 같은 중요 개념부터 지구의 속도, 과학자들이 외계 생명체를 찾는 영역, 번개의 온도, 세상에서 가장 깊은 바다, 특별한 능력이 있는 생물들, 인간 컴퓨터, 살아 있는 나무의 반전 등 분야를 넘나드는 최신 과학 정보까지 두루 다루고 있어요. 새로운 지식을 배우고 지적 호기심을 키워 나갈 수 있는 매력적인 과학 이야기가 가득하지요.','2017-06-29','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(213,'9788937889165','내일은 로봇왕 :본격 대결 과학로봇 만화','미래엔','글: 포도알친구 ;그림: 홍종현','https://image.aladin.co.kr/product/11879/14/cover200/8937889161_1.jpg','로봇에 담긴 과학 원리를 이해하고 나만의 로봇을 만들어 보자! 로봇에 대한 모든 것, 『내일은 로봇왕』 제3권. 본격 대결 과학로봇 만화 은 축구로봇과 배틀로봇이라는 흥미로운 소재를 중심으로, 로봇의 역사와 숨겨진 과학 원리를 쉽고 재미있게 전달하는 로봇 학습만화입니다. 매력적인 주인공들이 이끌어 나가는 재미있는 이야기와 정보 페이지의 풍부한 학습 내용을 통해 최초의 컴퓨터는 어떤 모습이었는지, 프로그래밍 언어가 어떻게 발달해 왔는지 알 수 있고, 컴퓨터 프로그래밍의 기초가 되는 이진법에 대한 내용도 배울 수 있습니다.\n\n그리고 이진법을 개발한 수학자 라이프니츠, 언플러그드 컴퓨팅 활동 등에 대한 정보도 얻을 수 있습니다. 또 로봇을 직접 만들며 살펴볼 수 있는 ‘로봇 키트’를 통해 로봇에 담긴 과학 원리를 좀 더 쉽게 이해할 수 있고, 키트 가이드에 소개된 모습과 다른 형태로 새롭게 조립하며 창의력를 키울 수 있습니다. 의 재미있는 이야기와 로봇 키트는 로봇을 좋아하는 아이들은 물론, 어렵게 느끼는 아이들도 로봇에 흥미를 붙이고 쉽게 이해할 수 있도록 돕는 훌륭한 친구가 될 것입니다.','2017-09-27','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(214,'9788930202336','Why? 컴퓨터 ','예림당','글: 조영선 ;만화: 그림수레','https://image.aladin.co.kr/product/716/62/cover200/8930202330_1.jpg','아이들에게 과학에 대한 놀라운 지식을 전해주는 「초등과학학습만화」 제4권 『WHY? 컴퓨터』. 아이들이 자신과 또래인 \'꼼지\'와 \'엄지\'가 컴퓨터 전문가 \'실리\'와 컴퓨터 프로그램 \'디버거\'와 함께하는 신나는 모험을 통해 과학에 대한 놀라운 지식을 재미있게 배워나가도록 구성한 과학학습만화다. 가정, 방송국, 공장, 우주선, 병원 등에서 중요한 역할을 하는 \'컴퓨터\'에 대해 다룬다. 컴퓨터에 대해 올바르게 이해하여 미래 첨단 시대를 준비하도록 이끌고 있다. 생생한 사진도 곁들여 이해를 도와준다.','2009-02-25','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(215,'9788930202657','Why? 뇌 ','예림당','글: 남춘자 ;만화: 박종성','https://image.aladin.co.kr/product/716/73/cover200/8930202659_1.jpg','아이들에게 과학에 대한 놀라운 지식을 전해주는 「초등과학학습만화」 제36권 『WHY? 뇌』. 아이들이 자신과 또래인 \'꼼지\'와 \'엄지\'가 영리하고 똑똑한 휴머노이드 \'해마\'와 함께하는 신나는 모험을 통해 과학에 대한 놀라운 지식을 재미있게 배워나가도록 구성한 과학학습만화다. 우리가 자고 있을 때조차도 쉬지 않고 일하는 \'뇌\'에 대해 탐구한다. 뇌의 구조와 역할부터 뇌에 좋은 음식까지 뇌에 대한 모든 것을 담아냈다. 생생한 사진도 곁들여 이해를 도와준다.','2008-01-10','49609','국내도서>어린이>과학/수학/컴퓨터>우리 몸',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(216,'9788949111278','(마녀 위니의)새 컴퓨터','비룡소','코키 폴 그림;노은정 옮김','https://image.aladin.co.kr/product/51/20/cover200/k352535735_1.jpg','마녀 위니가 나오는 시리즈 동화 \'마녀 위니의 새 컴퓨터\'편. 마녀 위니에게 새 컴퓨터를 샀다. 마법책이나 요술 지팡이가 없어도 마우스만 한 번 클릭하면 척척 원하는 대로 마법이 걸렸다. 위니는 잔뜩 신이 나서 마법책이랑 요술 지팡이를 내다버렸지만 한밤 중에 마우스에게 심술이난 고양이 윌버가 마우스를 마구 굴리는 바람에 사고가 생겼다. 위니가 아침에 일어났을 때는 컴퓨터와 윌버가 사라져 버린 것이다. 위니는 마법책이랑 요술 지팡이도 없는데 이 사고를 해결할 수 있을까?','2004-08-01','35140','국내도서>유아>그림책>나라별 그림책>외국 그림책',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(217,'9788958075912','수학특성화중학교 ','뜨인돌','지은이: 이윤원,김주희','https://image.aladin.co.kr/product/7236/84/cover200/8958075910_1.jpg','공부의 흥미와 재미를 경험하게 해주는 수학소설『수학특성화중학교』제1권. 수학 영재 집합소 수학특성화중학교에 첫 신입생들이 입학했다. 아이들은 매일같이 던져지는 수학 문제와 씨름하던 중 비밀조직 ‘제로’의 존재를 알게 된다. 제로에 다가가면 갈수록 그 안에 숨은 거대한 비밀이 드러나는데….','2015-12-24','50759','국내도서>청소년>청소년 문학>청소년 소설',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:05'),(218,'9788983718914','김상욱의 양자 공부 :완전히 새로운 현대 물리학 입문 ','사이언스북스','지은이: 김상욱','https://image.aladin.co.kr/product/11380/16/cover200/s162534273_1.jpg','『김상욱의 양자 공부』 저자가 《과학동아》에서 연재해 과학 독자들의 사랑을 한 몸에 받은 《양자 역학 좀 아는 척!》의 에피소드들을 취합하고 발전시켜 만든 역작이다. 어려운 개념을 정확한 정의와 유머러스한 비유를 통해 차근차근 이해시켜 나가는 방식으로 대중으로부터 압도적 신뢰를 받아 온 저자의 개성을 극적으로 살린 책이다. 또한 흥미와 호기심을 자극하는 가상 드라마 「양자 역학의 하루」를 통해 양자 역학의 진입 장벽을 과감히 허물었다. 100년에 걸친, 이해 불가능한 현상들을 설명하려는 물리학자들의 흥미진진한 이야기를 따라가다 보면 자연스레 양자적 사고 능력을 갖출 수 있다.','2017-12-08','51296','국내도서>과학>물리학>양자역학',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(219,'9791156396635','코딩맨 :히어로 액션 코딩 학습 만화','스튜디오다산','글: 송아론,그림: 김기수','https://image.aladin.co.kr/product/12931/52/cover200/k472532488_1.jpg','코딩맨과 함께하는 흥미로운 코딩 학습!\n\n코딩이 반드시 필요한 세상 속에서 하루가 다르게 실력을 키워 가는 이 시대의 영웅과 같은 인물 유강민을 주인공으로 한 학습만화 『코딩맨』 제1권 《버그킹덤의 차원 침략》. 만화를 통해 코딩에 대한 흥미, 코딩 공부를 위한 동기와 의욕을 만들어가고 코딩이 재미있는 것이고, 왜 반드시 배워야 하는 분야인지 알게 해준다.\n\n인간이 살고 있는 차원 너머의 차원. 그곳은 인간의 감정 따윈 찾아볼 수 없는, 프로그램으로 코딩된 종족들의 공간이 존재한다. 한 무리가 인간 세계를 버그로 감염시키려 한다. 버그킹덤! 그들을 막기 위해 분투 중인 디버깅! 그리고 갑자기 알 수 없는 능력이 생겨 버린 평범한 초등학생 유강민. 이후 강민의 주변에서 자꾸만 이상한 일들이 벌어지는데…….','2018-01-26','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(220,'9788934933151','아웃라이어 :성공의 기회를 발견한 사람들 ','김영사','말콤 글래드웰 [지음] ;노정태 옮김','https://image.aladin.co.kr/product/301/7/cover200/8934933151_3.jpg','\'특별한 기회\'와 \'역사문화적 유산\'의 두 측면에서 상위 1%의 성공비결을 분석!\n\n이 책은 글로벌 베스트셀러인『티핑포인트』『블링크』의 저자 말콤 글래드웰의 저서이다. 컴퓨터 황제 빌 게이츠, 프로그래밍 귀재 빌 조이, 록의 전설 비틀스 등 이 시대 아웃라이어들이 성공할 수 있었던 결정적인 비밀을 파헤친다. 그 동안 우리가 보지 못했던 성공의 색다른 측면을 제시한다.\n\n제목은 \'아웃라이어\'는 본체에서 분리되거나 따로 분류되어 있는 물건, 또는 표본 중 다른 대상들과 확연히 구분되는 통계적 관측치를 의미한다. 이 책에서는 보통 사람들의 범주를 뛰어넘은 특별한 사람, 즉 천재를 뜻한다. 본문은 21세기 성공의 새로운 모델로 이 아웃라이어를 주목하며 성공과 부의 비밀을 밝힌다.\n\n1부에서는 성공한 사람들의 흔해 빠진 최루성 신파극이나 평범한 사람이 감히 접근접할 수 없는 영웅들의 무용담 대신, 성공하는 사람들이 누렸던 행운과 그들이 태어난 시대적 배경 및 세대 등의 요인을 객관적으로 살펴본다. 2부에서는 개인은 결국 사회라는 문화적 테두리 안에서 성공할 수 밖에 없다는 것을 강조한다.','2009-01-15','70216','국내도서>자기계발>성공>성공학',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(221,'9791187513810','미래가 온다, 뇌과학 ','와이즈만 Books','김성화,조승연 그림','https://image.aladin.co.kr/product/20182/47/cover200/k712635640_1.jpg','와이즈만 미래과학 3권. 뇌 과학의 기초를 초등학생도 쉽게 이해할 수 있도록 서술한 국내 최초의 초등용 해설서이다. 뇌 과학의 역사와 미래 전망에 이르기까지 인간의 뇌를 둘러싼 다양한 궁금증들을 쉽고 재미있게 소개했다.','2019-08-05','49609','국내도서>어린이>과학/수학/컴퓨터>우리 몸',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(222,'9788949150246','신기한 스쿨 버스:키즈','비룡소','조애너 콜 글;브루스 디건 그림','https://image.aladin.co.kr/product/30/16/cover200/8949150247_2.jpg','기존의 ｀신기한 스쿨버스｀ 시리즈가 다소 어렵게 느껴졌을 초등 저학년생이나 취학전 아이들을 대상으로 기획된 시리즈. 또한 이 책은 TV에서 방영되었던 ｀신기한 스쿨버스｀ 프로그램을 간추려 책으로 만든 것이어서 내용은 더욱 쉬워지고 글의 이야기적 요소가 더 증가되었다.','2001-11-12','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',2,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(223,'9788964620878','야밤의 공대생 만화 ','뿌리와이파리','지은이: 맹기완','https://image.aladin.co.kr/product/10044/20/cover200/8964620879_2.jpg','이름도 몰랐던 과학자들이 친근해지는 진짜 과학-만화책!\n\n재미와 과학, 둘 다 놓치지 않으려는 노력의 산물『야밤의 공대생 만화』. 자라나는 새싹들에게 ‘노잼’ 과학만화를 읽혀서 어린 시절부터 트라우마를 겪고 ‘과알못’의 길을 걷지 않도록, 서울대 공대생이 직접 구상하고 글과 그림까지 혼자 그린 책이다. 복학 직전에 서울대 커뮤니티 사이트 ‘스누라이프’에 트랜지스터의 발명자에 대한 에피소드를 올린 것을 시작으로 페이스북과 카카오플러스친구, 딴지일보에서 1년 반 동안 연재하며 인기 만화로 자리 잡았고, 책으로 읽고 싶다는 독자들의 바람에 힘입어 연재분 29편을 수정·보완하여 출간했다.\n\n유명 과학자부터 조금은 생소한 과학자까지, 고등학교 때 배우는 미적분부터 알파고까지, 과학사의 중요한 인물과 사건을 시시콜콜 나열하거나 무리해서 어려운 과학을 설명하려 하기보다, 원리와 의미를 짧고 깔끔하게 설명하면서도 재미있게 읽고 핵심을 이해하며 넘어갈 수 있도록 구성했다. 인터넷 유머와 센스 넘치는 패러디로 전달하면서 이름만 알았던 과학자들의 새로운 인간미를 접할 수 있게 했다.','2017-07-07','51002','국내도서>과학>기초과학/교양과학',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(224,'9788980408443','받은 편지함:남찬숙 장편 동화','우리교육','남찬숙 지음;황보순희 그림','https://image.aladin.co.kr/product/57/36/cover200/8980408447_1.jpg','어려운 가정 상황 때문에 마음이 지쳐있는 순남이는 교실에서도 아무도 말을 걸어주지 않는 외톨이다. 컴퓨터 수업 시간에 메일보내기 실습을 하면서, 읽고 있던 동화책의 작가에게 이메일을 보내게 된다. 그리고 얼마 후, 작가 선생님은 순남이에게 답신을 보내준다.\n\n\n\n작가 선생님과 메일을 쓰면서 순남이는 자기 이름 대신 반에서 가장 똑똑하고 예쁜 혜민이의 이름을 쓴다. 순남이는 혜민이 이름으로 메일을 쓰면서 대리 만족을 느끼기도 하고, 선생님의 따뜻한 답신에 용기를 얻는다. 혜민이는 우연히 책을 매개로 순남이와 친구가 된다.\n\n\n\n작가 선생님과 편지를 주고받으면서 순남이에게는 좋은 일이 많이 생긴다. 혜민이와 친구가 되고, 자신의 꿈을 이룰 수 있는 방법도 찾게 된다. 그렇지만, 거짓은 언제나 탄로가 난다. 작가 선생님은 순남이에게 책을 보내려다가 순남이가 이름을 속이고 메일을 보냈다는 사실을 알게 된다.\n\n\n\n작은 거짓말에서 시작한 일들이 일으키는 사건과 그 사이에서 느껴지는 긴장감, 섬세하게 표현된 아이의 내면이 공감을 불러 일으킨다. 타인과 소통하면서, 자기 자신을 있는 그대로 인정하게 되고, 좀더 나은 미래를 향해 나아갈 용기를 얻는 성장 이야기다.','2005-07-15','48874','국내도서>어린이>동화/명작/고전>국내창작동화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(225,'9788901229614','초예측 :세계 석학 8인에게 인류의 미래를 묻다 ','웅진씽크빅','유발 하라리,정현옥 옮김','https://image.aladin.co.kr/product/18116/28/cover200/8901229617_1.jpg','세계 석학들의 대담한 고찰을 통해 인류의 미래를 위한 최선을 길을 찾다!\n\n20세기 부와 평화를 담보했던 기성 체제는 빠른 속도로 무너져 내리고, 인류 문명이 질적으로 완전히 다른 국면에 진입할 순간이 머지않아 보이는 지금, 몇몇 숫자와 조어로 포장된 단기 예측보다 변화의 방향과 강도를 입체적으로 분석해 미래를 적극적으로 사유할 수 있게 돕는 지혜가 절실하게 필요하다. 우리 문명에 다가올 지각변동을 날카롭게 통찰하는 세계 석학 8명과의 대담을 엮은 『초예측』은 전환의 길목에서 결정된 미래를 수용하는 대신 더 나은 미래를 만들어나가고자 하는 모든 이들에게 훌륭한 길잡이가 되어준다.\n\n《사피엔스》의 저자로 인류의 앞날을 고민하는 역사학자인 유발 하라리와 퓰리처상 수상작 《총, 균, 쇠》의 저자이며 세계적 문명 연구가인 재레드 다이아몬드를 비롯해 인공지능 연구가 닉 보스트롬, 인재론 권위자 린다 그래튼, 경제학 대가 다니엘 코엔, 노동법 전문가 조앤 윌리엄스, 인종사학자 넬 페인터, 전 미 국방부 장관 윌리엄 페리가 놈 촘스키, 마이클 샌델, 짐 로저스 등 세계 주요 인사들과 단독 인터뷰를 해온 경험 풍부한 국제 저널리스트 오노 가즈모토의 진행으로 미래에 대한 생각을 독자들과 나눈다.\n\n최악의 경우를 포함해 모든 가능성을 제시하는 것이 학자로서의 소명이라고 밝히는 유발 하라리는 과학기술의 급속한 발전으로 대부분의 인간이 경제적, 정치적 가치를 잃고 ‘무용 계급(useless class)’으로 전락할 수 있다고 경고하면서 위기가 현실이 되기 전에 지금 바로 움직일 것을 당부한다. 재레드 다이아몬드는 앞으로 우리에게 닥칠 세 가지 위협으로 신종 감염병, 테러리즘, 타국으로의 이주를 꼽으며 그 원인이 되는 국가 간 격차를 줄이기 위해 국제적 노력이 이뤄져야 한다고 주장하는 등 변화에 휩쓸리지 않고 위험에 맞서 최선의 길을 찾을 수 있는 힘을 선사하는 석학들의 통찰을 만나볼 수 있다.','2019-02-08','51008','국내도서>사회과학>미래학',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(226,'9788930237970','Why? 게임 과학','예림당','조영선 (지은이), 이영호 (그림), 윤형섭 (감수)','https://image.aladin.co.kr/product/25681/18/cover200/8930237975_1.jpg','상상하는 모든 것이 현실이 되는, 게임에 관한 모든 것!\n\n즐거움을 추구하는 인간의 본능 때문에 인류의 시작부터 행해져 왔던 게임은 컴퓨터 기술이 발전하면서 더 재미있어지고, 더 대중화되었다. 심지어는 직접 하지 않고 마치 스포츠를 관람하듯 다른 이들이 게임하는 모습을 보는 것을 즐기는 사람도 늘어났다.\n하지만 현실에서는 할 수 없었던 것들을 게임 속 캐릭터가 대신하며 대리 만족을 느낄 수 있기 때문에 게임에 한번 중독되면 빠져나오기 어려울 수도 있다. 그렇게 되면 일상생활뿐 아니라 건강에도 악영향을 미칠 수 있으며, 가족, 친구 등 현실의 소중한 것을 잃을 수도 있다.\n2019년 세계 보건 기구(WHO)는 게임 이용 장애(게임 중독)를 질병으로 규정하며, ‘질병 코드’를 부여했다. 하지만 코로나 19로 위기를 겪고 있는 2020년, WHO의 사무총장은 사회적 거리두기를 강조하며, 집에서 즐길 수 있는 여가 혹은 치유의 목적으로 게임을 권장하는 상반된 모습을 보이기도 했다. 이처럼 게임에 대한 논란은 여전히 뜨겁다. \n게임은 이제 단순한 취미를 넘어 교육, 의료, 생산 등 다양한 산업과 융합하여 미래를 주도하는 차세대 산업으로 주목받고 있기에, 컴퓨터 게임에 대한 다양한 정보를 찾고 그 원리를 이해하는 것은 다가올 미래를 준비하는 좋은 공부가 될 것이다. 또한 게임 자체가 무조건 나쁜 것만은 아니므로 적절한 기준을 세우고, 게임 하는 시간을 지킨다면 삶의 적절한 활력소가 될 것이다.\n〈Why? 게임 과학〉에는 온라인 컴퓨터 게임을 사랑하는 소녀 엄지와 텃밭 가꾸는 것을 좋아하는 소년 꼼지가 등장한다. 게임을 탐탁잖게 여기는 꼼지에게 엄지는 게임의 매력에 대해 알려 준다. 최초의 게임 스페이스 워부터 최신 VR 게임까지 게임의 역사에 대해서 훑어보고, 게임이 만들어지는 과정, 원리에 대해 알고 나만의 게임까지 직접 만들며 게임에 점점 흥미를 갖게 되는 꼼지! 과연 꼼지의 마음은 어떻게 바뀌었을까?','2020-11-10','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(227,'9788983718686','백년 목 :100년 가는 목 만드는 단 하나의 방법 ','사이언스북스','지은이: 정선근','https://image.aladin.co.kr/product/12388/98/cover200/8983718684_1.jpg','목 디스크는 예방하고, 거북목은 수술 없이 치료한다!\n\n《백년 허리》로 요통과 허리 디스크의 비밀을 밝혀내고, 허리 망치는 운동과 자세 대신 허리의 보증 기간을 100년으로 늘리는 운동과 자세를 제안해 온 정선근 서울대 의대 재활 의학과 주임 교수가 이번에는 목 디스크 해방에 도전한다. 『백년 목』에서 저자는 전작에서 소개한 허리와 목 디스크에 대한 의 ㆍ 과학 논의를 심화 발전시켜 소개한다. \n\n목과 허리 디스크가 발생하는 원인, 목과 허리 디스크 손상으로 발생하는 방사통과 연관통의 메커니즘, 목 디스크와 허리 디스크 구조의 해부학적, 재활 의학적, 진화적 연관성, 목과 허리 디스크의 문제를 해결하기 위해 노력해 온 100여 년에 걸친 의학계의 논쟁과 연구 성과 등을 일반 독자, 일반 환자의 눈높이에서 알기 쉽게, 그리고 현장감 넘치는 임상 사례와 함께 들려준다.\n\n1, 2, 3장에서는 목 디스크가 손상되는 이유를 설명한다. 하루 종일 노트북 컴퓨터와 씨름하는 전문직, 사무직 종사자들의 고된 일과, 스트레스와 우울증, 수면, 운전, 텔레비전 시청 같은 일상 생활, 교통 사고나 스포츠 손상 등이 어떻게 목 디스크를 파괴하는지를 정리하고 있다. 4, 5, 6장은 목 디스크 병의 증상에 대한 내용을 담았다. \n\n7, 8, 9장은 목 디스크 병에 대한 진단과 치료 방침의 결정이 결코 간단치 않음을 설명하면서 개념 있는 전문가와 개념 있는 환자가 만날 때 최선의 결과가 보장된다고 이야기하고, 아무런 효과도 없는, 해로울 수도 있는, 턱없이 비싼, 각종 혹세무민 치료법들이 이 시대를 풍미하는 복잡한 이유도 짚어 본다. 10, 11장은 목 디스크 병을 예방하고 치료하기 위해 독자들이 스스로 해야 할 것들을 알려 준다. 특히 좋은 운동으로 잘못 알려져 있는 목 디스크 운동들의 오류를 지적하고 진짜로 좋은 운동은 어떤 것인지 명쾌하게 소개하고 있다.','2017-11-30','53550','국내도서>건강/취미>질병치료와 예방>허리/디스크/척추/통증',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(228,'9791156394211','Who? 안철수 =Ahn Cheolsoo ','스튜디오 다산','글: 최재훈 ;그림: 정석호','https://image.aladin.co.kr/product/10439/50/cover200/k992536391_1.jpg','사회 기여를 위한 그의 도전은 계속된다!\n\n오늘날의 대한민국을 비롯한 이 시대를 이끄는 ‘현대 대표 인물’의 이야기로 구성되어 있는『Who? Special』시리즈 안철수 편. 너무 먼 시대에서 활동하여 공감하기 어려운 기존 위인전의 인물이 아닌, 현재 사회 전반에 커다란 영향력을 미치고 있는 인물의 삶을 보여줌으로써 보다 실질적인 \'롤모델\'을 구현했습니다. 화려하게만 보였던 성공 뒤에 숨겨져 있는 고통은 물론, 한때는 누구보다 나약했고 실패에 절망했던 이야기가 그려집니다.\n\n컴퓨터 백신 개발자에서 국내 최고 벤처 기업 경영자, 대한민국을 이끄는 정치 리더까지! 눈앞에 보이는 안정된 삶을 뒤로하고 도전에 도전을 거듭한 안철수를 만나보세요. 책 속 부록인 ‘통합지식 플러스’ 코너에서는 의사에서 프로그래머, 또 기업인을 거쳐 국회 의원으로 거듭난 안철수의 노력에 대해 자세히 확인할 수 있습니다. 또한 권말 ‘어린이 진로 탐색’에서는 ‘국회 의원’이라는 직업을 중심으로 진로를 탐색합니다.','2017-03-06','48902','국내도서>어린이>문화/예술/인물>한국인물',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(229,'9788965481676','구스범스','고릴라박스','R. L. 스타인 지음 ;신인수 옮김','https://image.aladin.co.kr/product/4685/6/cover200/8965481678_1.jpg','자신의 부정적 감정을 극복하도록 안내해주는 책!\n\n아이들에게 오싹한 즐거움을 선사해주는 본격 호러 동화 『구스범스』 제5권 《악령을 부르는 머리》. 영어로 ‘소름’이라는 뜻을 지난 구스범스 시리즈는 1992년 미국에서 첫 출간된 이래 20여 년 동안 꾸준한 사랑을 받아, 어린이가 뽑은 가장 재미있는 책에 수여하는 ‘니켈로디온 상’을 3번이나 받은 작품이다. 악마 같은 저주인형, 죽지 않는 악마가면, 반인반수 늑대인간 등 아이들에게 친숙한 초자연적 존재들을 통해 공포를 조성한다. 어린이가 느낄 수 있는 낯선 것에 대한 스트레스, 분리 불안, 형제자매나 친구 사이의 경쟁 등을 어린이의 시각에서 흥미롭게 다뤘다. \n\n이모의 초대로 떠난 정글의 섬에서 마크는 청천벽력 같은 사실을 알게 된다. 이모는 행방불명되었고, 마크를 반겨 주었던 정글 섬 과학자는 잔인한 악당이었던 것이다. 설상가상, 악당은 마크에게 고대 정글의 마법을 내놓으라고 협박한다. 마크는 주문을 외우면 엄청난 힘이 생긴다는 정글 마법의 주문을 알고 있는 특별한 아이라는 사실을 처음으로 알게 된다. 칠흑 같은 정글의 밤, 기괴한 새의 울음소리, 끈질기게 뒤를 밟는 추격자가 마크는 두렵기만 하는데….','2014-09-25','48877','국내도서>어린이>동화/명작/고전>외국창작동화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(230,'9791187513858','미래가 온다 인공지능 ','와이즈만 Books','김성화,이철민 그림','https://image.aladin.co.kr/product/21418/67/cover200/k102636896_2.jpg','생각하고, 말하고, 스스로 배우는 기계\n인공 지능과 함께 살기 해설서\n\n“인공 지능의 역사와 미래 전망에 이르기까지 다양한 궁금증에 대한 해답”\n\n기계가 사람만큼 똑똑해질 수 있을까?\n지능이 있고, 생각하고, 의식이 있는 인공 지능이 올까?\n강한 인공 지능이 언제 올까?','2019-11-20','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(231,'9791170181699','(퀴즈!) 과학상식 :미래 과학 ','글송이','차현진 지음','https://image.aladin.co.kr/product/693/70/cover200/k682434891_1.jpg','퀴즈로 배우는 미래과학 이야기『퀴즈! 과학상식: 미래과학』. 투명망토가 실제로 있는지, 가짜 태양을 만들 수 있는지, 스스로 흠집을 치료하는 자동차가 있는지 정말 궁금하지요? 말썽꾸러기 주인공들이 벌이는 엉뚱한 사건을 통해 미래과학에 대한 여러분의 궁금증을 확~ 풀어 보세요.','2015-11-30','112080','국내도서>어린이>학습만화',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:34:06'),(232,'9788930237925','Why? 암호 화폐와 블록체인 ','예림당','글: 조영선 ;그림: 이준희','https://image.aladin.co.kr/product/21413/30/cover200/8930237924_1.jpg','상상 그 이상의 혁신을 이끌 IT 신기술\n\n암호 화폐, 그리고 블록체인. 이름부터 어려운 이 두 가지가 대체 뭔데 전 세계가 들썩이고 있는 걸까? 암호 화폐는 말 그대로 암호화 기술이 적용된 전자 화폐다. 그리고 블록체인은 암호 화폐의 기본 원리라고 할 수 있다. 거래 내역이 담긴 하나하나의 블록을 쇠사슬 모양으로 연결하여 이를 복제해 수많은 컴퓨터에 저장하는 방식인데, 이는 지금까지 보지 못한 신개념 시스템이다. 거래에 참여하는 모든 사람들이 이를 검증하기 때문에 위조나 변조가 이루어지기 힘들고 보안이 매우 뛰어나다. \n\n〈Why? 암호 화폐와 블록체인〉에서는 대표적인 암호 화폐인 비트코인을 개발한 사토시 나카모토를 컴퓨터 동아리 멤버로 설정하고 개발자들의 의도와 다르게 변해 가는 암호 화폐에 위기감을 느껴 앞으로의 행보를 결정하기 위해 아이들의 마음을 들여다본다는 내용을 담고 있다. 이 과정에서 암호 화폐와 블록체인에 관한 보다 자세한 내용은 물론, 올바른 활용 방안과 미래 네트워크 기술과의 융합을 예측해 볼 수도 있다.\n\n4차 산업 혁명 시대를 이끄는 다양한 네트워크 기술의 발달과 블록체인의 뛰어난 보안성이 더해진다면 상상 이상의 눈부신 변화가 일어날 것이다. 이 책을 통해 새로운 기술에 한발 다가가 무한한 가능성을 지닌 미래의 주역이 되어 보는 건 어떨까?','2019-10-25','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(233,'9788930202671','Why? 정보통신 - 2판','예림당','조영선','https://image.aladin.co.kr/product/716/69/cover200/8930202675_1.jpg','전문가가 꼼꼼히 감수한 과학 지식을 어린이들이 좋아하는 올컬러 만화로 재미나게 풀어낸 「초등과학학습만화 WHY」시리즈 『정보통신』. 상품의 가치를 갖고 있는 정보를 시작으로, 우리 실생활을 보다 편리하게 도와주는 정보통신의 기술을 알려준다. 재미난 이야기와 익살맞은 그림이 충실히 담겨있어 과학 지식과 조화를 잘 이루어 재미나면서도 깊이 있는 과학을 접할 수 있다.','2008-01-10','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(234,'9791185402420','(과학이 톡톡 쌓이다!) 사이다 =Sci-da','상상아카데미','글: 정원영,김하나','https://image.aladin.co.kr/product/28170/88/cover200/k922835563_2.jpg','국내 최대 과학관의 최고 과학자들이 \n들려주는 진짜 과학 이야기\n\n『과학이 톡톡 쌓이다! 사이다 2 : 인공지능』\n4차 산업혁명 시대가 도래했고, 이제는 인공지능이 우리의 일상을 크게 차지하고 있다. 하지만 여전히 인공지능이 무엇이고, 어떤 역할을 하는지 이해하기 어려워하는 어린이들이 많다. 이를 위해 인공지능에 대해서 꼭 알아야 할 내용을 남녀어린이와 인공지능이 친구처럼 편안하게 대화하는 형식에 담아 구성하였다. 인간의 뇌에서부터 딥 러닝, 약인공지능과 강인공지능, 자율 주행 자동차와 빅데이터까지 인공지능과 관련된 모든 내용에 대해 묻고 답한다. 또 추상적이고 어려운 개념들을 그림으로 표현해 어린이 독자들이 쉽게 이해할 수 있도록 구성하였다.','2021-10-25','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(235,'9788935208647','생각하지 않는 사람들 :인터넷이 우리의 뇌 구조를 바꾸고 있다! ','청림출판','니콜라스 카 지음 ;최지향 옮김','https://image.aladin.co.kr/product/21025/75/cover200/k282636570_1.jpg','\'스마트\' 시대, 우리는 더 똑똑해지고 있습니까?\n\n인터넷, 정보기술, 스마트 기기의 발달은 우리의 정보와 지식을 습득하는 방식에 거대한 변화를 불러일으켰다. 책 한 권을 다 읽고 밑줄을 그으며 지식을 얻기보다 인터넷 검색을 통해 단 몇 분 만에 손쉽게 정보를 습득할 수 있는 세상이 열린 것이다. 우리 주변에는 놀라울 정도로 많은 양의 정보들이 차고 넘치며, 사람들은 걸어 다니면서도 정보를 찾아낸다. 그렇다면 우리는 정말 더 스마트해졌을까? IT 전문가이자 저명한 칼럼리스트인 저자 니콜라스 카가『생각하지 않는 사람들』을 통해 디지털 기기에 종속된 이후 우리의 사고하는 방식은 어떻게 변화하고 있는지, 글을 쓰는 방식과 읽는 방식은 어떻게 변화하고 있는지를 밝힌다.','2019-10-17','3065','국내도서>경제경영>경제학/경제일반>경제이야기',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(236,'9791186872536','분수와 소수','어스본코리아','로지 디킨스 (지은이), 베네데타 죠프레, 엔리카 루시나 (그림)','https://image.aladin.co.kr/product/8666/19/cover200/k782535080_1.jpg','쉽고 재밌는 『분수와 소수』는 초등 학습과 연계된 컴퓨터와 코딩부터 수학, 과학과 예술, 스포츠, 역사까지 다양한 주제를 담고 있습니다. 열면 또 열리는 열면 또 열리는 플랩 속 플랩은 ‘호기심’을 돋우고, ‘집중력’을 기르고, 점점 더 깊은 ‘탐구심’으로 이어질 수 있도록 도와줍니다.어려운 지식책은 이제 그만! 기본 개념부터 원리를 이해하는 단계까지, 보다 깊이 있는 학습 효과를 함께 해보세요.','2016-07-05','48871','국내도서>어린이>과학/수학/컴퓨터>초등 수학',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(237,'9788971849866','휴대폰 전쟁 ','푸른숲','로이스 페터슨 지음 ;고수미 옮김','https://image.aladin.co.kr/product/3298/39/cover200/897184986x_1.jpg','『휴대폰 전쟁』는 휴대폰 중독을 다룬 청소년 소설이다. ‘접속’ 상태에서만 자신의 존재감을 느끼는 중학생 다리아를 통해 휴대폰 중독의 위험성을 일깨워 준다. 요즘 청소년들의 일상을 섬세하면서도 매우 사실적으로 그리고 있어 휴대폰 중독의 현주소를 생생하게 보여주고 있으며 휴대폰 사용 습관을 되돌아 보게 한다.','2013-10-30','50759','국내도서>청소년>청소년 문학>청소년 소설',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(238,'9791165211004','팜 :기발한 상상력이 가득한 판타지 코딩과학동화','길벗','글: 홍지연 ;그림: 지문','https://image.aladin.co.kr/product/23590/25/cover200/k542638327_1.jpg','코딩과학동화 〈팜〉 두 번째 이야기!\n이번엔 하늘 농장이다!\n\n초등학생에게 코딩과 과학을 쉽게 알려주기 위해 기획된 코딩과학동화 《팜》 시리즈의 두 번째 이야기 ‘하늘 농장’ 편이 출간되었습니다. \n\n1권 지하 농장에 이어서, 2권에서는 하늘 농장이라는 전혀 다른 공간으로 이동합니다. 쌍둥이 형제 주니와 거니는 하늘 음악당, 태양 가림막 우산, 액괴 놀이방, 하늘 배 주차타워 등 하늘 농장에 필요한 장소와 발명품들을 만들면서 하늘 농장을 가꾸기 시작합니다. 그렇게 평화롭게(!) 지내려던 찰나 마른 하늘의 날벼락, 천둥 번개가 들이닥쳐 하늘 농장이 돌덩이처럼 굳어버리는데요.\n\n주니와 거니가 이 위기를 극복하는 과정에서 아이들은 자연스럽게 코딩 개념을 접하게 됩니다. 특별히 2권에서는 ‘네트워크, 센서, 컴퓨터 바이러스와 백신, 배열과 리스트, 이진수’ 같은 조금 더 확장된 컴퓨터과학 개념을 담았습니다.\n\n한층 더 재밌는 줄거리와 상상력 넘치는 그림, 덤으로 코딩에 대한 지식까지 얻을 수 있는 《팜》 두 번째 이야기를 지금 만나 보세요!\n\n[줄거리]\n지하 농장에서 거대 나무줄기를 타고 하늘로 올라 온 거니와 주니. 거대 나무줄기 끝에는 아무도 살지 않는 하늘 정원이 있다. 하늘 정원에 도착한 주니는 하늘 농장으로 만들어 살기를 원한다. 하지만 시작도 하기 전에 구름 폭탄을 발로 밟아 위기를 맞이한다. 우여곡절 끝에 구름 폭탄을 해체하고 본격적으로 하늘 농장을 가꾸기 시작한 주니와 거니. 뜨거운 태양 빛을 막는 자동 센서 태양 가림막을 시작으로 팝콘 게임방을 대신할 액괴 놀이방, 침입자를 감지하는 하늘 배, 기린의 하프 연주를 들을 수 있는 하늘 음악당 등 하늘 농장은 점차 제 모습을 갖춰간다. 그렇게 평화롭던 하늘 농장에 갑자기 천둥 번개가 내리치더니 번개가 닿은 곳은 모두 돌덩이로 변해 가는데...','2020-04-02','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(239,'9791133442003','도티&잠뜰 코딩','대원씨아이','글: 유경원 ;그림: 최대식','https://image.aladin.co.kr/product/10349/23/cover200/k632536680_1.jpg','컴퓨터 속 매트릭 랜드에 살고 있는 인기 캐릭터 도티와 잠뜰! 인기 투표에서 꼴찌를 한 도티가 영원히 사라질 위기에 처합니다. 도티가 살아남을 방법은 딱 하나! 현실 세계의 인간 팬을 데리고 코딩 하트를 찾아내야 해요. 코딩 하트가 있는 곳으로 가기 위해 지도와 나침반을 찾는 과정에서 사사건건 훼방을 놓는 도티의 닮은꼴, 더티의 정체는 무엇일까요? 도티와 잠뜰은 무사히 코딩 하트를 찾을 수 있을까요? \n\n『도티&잠뜰 코딩』 방과후 학교 시리즈 《컴퓨터 편》은 2018년부터 소프트웨어 교육이 의무화되면서 어린이들이 꼭 알아야 할 컴퓨터와 코딩 학습 내용을 재미있게 담아냈습니다. 재미있는 스토리를 읽기만 해도 컴퓨터와 코딩의 기초를 확실히 이해할 수 있습니다. 도티, 잠뜰과 함께하는 흥미진진한 모험을 통해 컴퓨터 프로그래밍에 흥미를 느껴 보세요!','2017-02-20','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(240,'9788949150253','신기한 스쿨 버스:키즈','비룡소','조애너 콜 글;브루스 디건 그림','https://image.aladin.co.kr/product/30/16/cover200/8949150255_2.jpg','유아들을 위한 재미있는 과학 동화책. 우리는 신기한 스쿨 버스를 타고 사막 동물들을 구하러 갔어요. 그런데 사막에 도착하자마자 배고픈 길달리기새가 우리를 먹으려고 따라왔어요. 우리는 오늘 견학을 무사히 마칠 수 있을까요?.. 주인공 아이들의 신나는 사막 여행을 통해 사막 생물들이 살아가는 방법을... ','2001-11-12','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반',2,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45'),(241,'9791186872529','(쉽고 재밌는) 컴퓨터와 코딩 ','Usborne Korea','로지 디킨스 글 ;쇼 닐센 그림','https://image.aladin.co.kr/product/8665/86/cover200/k562535080_1.jpg','쉽고 재밌는 『컴퓨터와 코딩』은 초등 학습과 연계된 컴퓨터와 코딩부터 수학, 과학과 예술, 스포츠, 역사까지 다양한 주제를 담고 있습니다. 열면 또 열리는 열면 또 열리는 플랩 속 플랩은 ‘호기심’을 돋우고, ‘집중력’을 기르고, 점점 더 깊은 ‘탐구심’으로 이어질 수 있도록 도와줍니다.어려운 지식책은 이제 그만! 기본 개념부터 원리를 이해하는 단계까지, 보다 깊이 있는 학습 효과를 함께 해보세요.','2016-07-05','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩',1,1,2,'2026-07-13 10:29:07','2026-07-13 10:35:45');
/*!40000 ALTER TABLE `book_isbn_temp` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_keyword`
--

DROP TABLE IF EXISTS `book_keyword`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_keyword` (
  `keyword_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `keyword_name` varchar(100) NOT NULL,
  `keyword_type` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`keyword_id`),
  UNIQUE KEY `uk_book_keyword_keyword_name` (`keyword_name`),
  KEY `idx_book_keyword_keyword_type` (`keyword_type`),
  KEY `idx_book_keyword_status` (`status`),
  CONSTRAINT `chk_book_keyword_keyword_type` CHECK ((`keyword_type` in (_utf8mb4'GENRE_KEYWORD',_utf8mb4'HASHTAG'))),
  CONSTRAINT `chk_book_keyword_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_keyword`
--

LOCK TABLES `book_keyword` WRITE;
/*!40000 ALTER TABLE `book_keyword` DISABLE KEYS */;
/*!40000 ALTER TABLE `book_keyword` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_keyword_map`
--

DROP TABLE IF EXISTS `book_keyword_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_keyword_map` (
  `book_id` bigint unsigned NOT NULL,
  `keyword_id` bigint unsigned NOT NULL,
  `display_order` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`book_id`,`keyword_id`),
  KEY `idx_book_keyword_map_keyword_id` (`keyword_id`),
  KEY `idx_book_keyword_map_display_order` (`display_order`),
  CONSTRAINT `fk_book_keyword_map_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_book_keyword_map_book_keyword` FOREIGN KEY (`keyword_id`) REFERENCES `book_keyword` (`keyword_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_keyword_map`
--

LOCK TABLES `book_keyword_map` WRITE;
/*!40000 ALTER TABLE `book_keyword_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `book_keyword_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_page`
--

DROP TABLE IF EXISTS `book_page`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_page` (
  `book_page_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `book_id` bigint unsigned NOT NULL,
  `page_no` int unsigned NOT NULL,
  `page_content` longtext NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`book_page_id`),
  UNIQUE KEY `uk_book_page_book_id_page_no` (`book_id`,`page_no`),
  CONSTRAINT `fk_book_page_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=233 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_page`
--

LOCK TABLES `book_page` WRITE;
/*!40000 ALTER TABLE `book_page` DISABLE KEYS */;
INSERT INTO `book_page` VALUES (39,332,1,'테스트 1페이지 내용입니다.','2026-07-11 16:40:57',NULL),(40,332,2,'테스트 2페이지 내용입니다.','2026-07-11 16:40:57',NULL),(107,1110,1,'놓지 마 과학! 전자책 시연 페이지 1','2026-07-13 10:26:32',NULL),(108,1110,2,'놓지 마 과학! 전자책 시연 페이지 2','2026-07-13 10:26:32',NULL),(109,1110,3,'놓지 마 과학! 전자책 시연 페이지 3','2026-07-13 10:26:32',NULL),(110,1111,1,'Why? 소프트웨어와 코딩  전자책 시연 페이지 1','2026-07-13 10:26:32',NULL),(111,1111,2,'Why? 소프트웨어와 코딩  전자책 시연 페이지 2','2026-07-13 10:26:32',NULL),(112,1111,3,'Why? 소프트웨어와 코딩  전자책 시연 페이지 3','2026-07-13 10:26:32',NULL),(113,1112,1,'놓지 마 과학! 전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(114,1112,2,'놓지 마 과학! 전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(115,1112,3,'놓지 마 과학! 전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(116,1113,1,'그림 도둑, 준모  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(117,1113,2,'그림 도둑, 준모  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(118,1113,3,'그림 도둑, 준모  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(119,1114,1,'할머니, 어디 가요? 밤 주우러 간다! :옥이네 가을 이야기  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(120,1114,2,'할머니, 어디 가요? 밤 주우러 간다! :옥이네 가을 이야기  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(121,1114,3,'할머니, 어디 가요? 밤 주우러 간다! :옥이네 가을 이야기  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(122,1115,1,'Why? 컴퓨터 바이러스  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(123,1115,2,'Why? 컴퓨터 바이러스  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(124,1115,3,'Why? 컴퓨터 바이러스  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(125,1116,1,'게임 없이 못 살아!  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(126,1116,2,'게임 없이 못 살아!  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(127,1116,3,'게임 없이 못 살아!  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(128,1117,1,'Why? SNS =소셜 네트워크 서비스  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(129,1117,2,'Why? SNS =소셜 네트워크 서비스  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(130,1117,3,'Why? SNS =소셜 네트워크 서비스  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(131,1118,1,'쿠키런 반짝 미래 과학 상식 :나의 미래 생활 신기한 미래 과학 25  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(132,1118,2,'쿠키런 반짝 미래 과학 상식 :나의 미래 생활 신기한 미래 과학 25  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(133,1118,3,'쿠키런 반짝 미래 과학 상식 :나의 미래 생활 신기한 미래 과학 25  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(134,1119,1,'Why? 과학사건  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(135,1119,2,'Why? 과학사건  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(136,1119,3,'Why? 과학사건  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(137,1120,1,'사라진 물건의 비밀  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(138,1120,2,'사라진 물건의 비밀  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(139,1120,3,'사라진 물건의 비밀  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(140,1121,1,'Why? 미래과학  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(141,1121,2,'Why? 미래과학  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(142,1121,3,'Why? 미래과학  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(143,1122,1,'(초등학생이 알아야 할) 과학 100가지  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(144,1122,2,'(초등학생이 알아야 할) 과학 100가지  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(145,1122,3,'(초등학생이 알아야 할) 과학 100가지  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(146,1123,1,'내일은 로봇왕 :본격 대결 과학로봇 만화 전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(147,1123,2,'내일은 로봇왕 :본격 대결 과학로봇 만화 전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(148,1123,3,'내일은 로봇왕 :본격 대결 과학로봇 만화 전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(149,1124,1,'Why? 컴퓨터  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(150,1124,2,'Why? 컴퓨터  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(151,1124,3,'Why? 컴퓨터  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(152,1125,1,'Why? 뇌  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(153,1125,2,'Why? 뇌  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(154,1125,3,'Why? 뇌  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(155,1126,1,'(마녀 위니의)새 컴퓨터 전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(156,1126,2,'(마녀 위니의)새 컴퓨터 전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(157,1126,3,'(마녀 위니의)새 컴퓨터 전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(158,1127,1,'수학특성화중학교  전자책 시연 페이지 1','2026-07-13 10:34:05',NULL),(159,1127,2,'수학특성화중학교  전자책 시연 페이지 2','2026-07-13 10:34:05',NULL),(160,1127,3,'수학특성화중학교  전자책 시연 페이지 3','2026-07-13 10:34:05',NULL),(161,1128,1,'김상욱의 양자 공부 :완전히 새로운 현대 물리학 입문  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(162,1128,2,'김상욱의 양자 공부 :완전히 새로운 현대 물리학 입문  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(163,1128,3,'김상욱의 양자 공부 :완전히 새로운 현대 물리학 입문  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(164,1129,1,'코딩맨 :히어로 액션 코딩 학습 만화 전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(165,1129,2,'코딩맨 :히어로 액션 코딩 학습 만화 전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(166,1129,3,'코딩맨 :히어로 액션 코딩 학습 만화 전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(167,1130,1,'아웃라이어 :성공의 기회를 발견한 사람들  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(168,1130,2,'아웃라이어 :성공의 기회를 발견한 사람들  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(169,1130,3,'아웃라이어 :성공의 기회를 발견한 사람들  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(170,1131,1,'미래가 온다, 뇌과학  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(171,1131,2,'미래가 온다, 뇌과학  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(172,1131,3,'미래가 온다, 뇌과학  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(173,1132,1,'신기한 스쿨 버스:키즈 전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(174,1132,2,'신기한 스쿨 버스:키즈 전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(175,1132,3,'신기한 스쿨 버스:키즈 전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(176,1133,1,'야밤의 공대생 만화  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(177,1133,2,'야밤의 공대생 만화  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(178,1133,3,'야밤의 공대생 만화  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(179,1134,1,'받은 편지함:남찬숙 장편 동화 전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(180,1134,2,'받은 편지함:남찬숙 장편 동화 전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(181,1134,3,'받은 편지함:남찬숙 장편 동화 전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(182,1135,1,'초예측 :세계 석학 8인에게 인류의 미래를 묻다  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(183,1135,2,'초예측 :세계 석학 8인에게 인류의 미래를 묻다  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(184,1135,3,'초예측 :세계 석학 8인에게 인류의 미래를 묻다  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(185,1136,1,'Why? 게임 과학 전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(186,1136,2,'Why? 게임 과학 전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(187,1136,3,'Why? 게임 과학 전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(188,1137,1,'백년 목 :100년 가는 목 만드는 단 하나의 방법  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(189,1137,2,'백년 목 :100년 가는 목 만드는 단 하나의 방법  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(190,1137,3,'백년 목 :100년 가는 목 만드는 단 하나의 방법  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(191,1138,1,'Who? 안철수 =Ahn Cheolsoo  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(192,1138,2,'Who? 안철수 =Ahn Cheolsoo  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(193,1138,3,'Who? 안철수 =Ahn Cheolsoo  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(194,1139,1,'구스범스 전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(195,1139,2,'구스범스 전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(196,1139,3,'구스범스 전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(197,1140,1,'미래가 온다 인공지능  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(198,1140,2,'미래가 온다 인공지능  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(199,1140,3,'미래가 온다 인공지능  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(200,1141,1,'(퀴즈!) 과학상식 :미래 과학  전자책 시연 페이지 1','2026-07-13 10:34:06',NULL),(201,1141,2,'(퀴즈!) 과학상식 :미래 과학  전자책 시연 페이지 2','2026-07-13 10:34:06',NULL),(202,1141,3,'(퀴즈!) 과학상식 :미래 과학  전자책 시연 페이지 3','2026-07-13 10:34:06',NULL),(203,1142,1,'Why? 암호 화폐와 블록체인  전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(204,1142,2,'Why? 암호 화폐와 블록체인  전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(205,1142,3,'Why? 암호 화폐와 블록체인  전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(206,1143,1,'Why? 정보통신 - 2판 전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(207,1143,2,'Why? 정보통신 - 2판 전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(208,1143,3,'Why? 정보통신 - 2판 전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(209,1144,1,'(과학이 톡톡 쌓이다!) 사이다 =Sci-da 전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(210,1144,2,'(과학이 톡톡 쌓이다!) 사이다 =Sci-da 전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(211,1144,3,'(과학이 톡톡 쌓이다!) 사이다 =Sci-da 전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(212,1145,1,'생각하지 않는 사람들 :인터넷이 우리의 뇌 구조를 바꾸고 있다!  전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(213,1145,2,'생각하지 않는 사람들 :인터넷이 우리의 뇌 구조를 바꾸고 있다!  전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(214,1145,3,'생각하지 않는 사람들 :인터넷이 우리의 뇌 구조를 바꾸고 있다!  전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(215,1146,1,'분수와 소수 전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(216,1146,2,'분수와 소수 전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(217,1146,3,'분수와 소수 전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(218,1147,1,'휴대폰 전쟁  전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(219,1147,2,'휴대폰 전쟁  전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(220,1147,3,'휴대폰 전쟁  전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(221,1148,1,'팜 :기발한 상상력이 가득한 판타지 코딩과학동화 전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(222,1148,2,'팜 :기발한 상상력이 가득한 판타지 코딩과학동화 전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(223,1148,3,'팜 :기발한 상상력이 가득한 판타지 코딩과학동화 전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(224,1149,1,'도티&잠뜰 코딩 전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(225,1149,2,'도티&잠뜰 코딩 전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(226,1149,3,'도티&잠뜰 코딩 전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(227,1150,1,'신기한 스쿨 버스:키즈 전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(228,1150,2,'신기한 스쿨 버스:키즈 전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(229,1150,3,'신기한 스쿨 버스:키즈 전자책 시연 페이지 3','2026-07-13 10:35:45',NULL),(230,1151,1,'(쉽고 재밌는) 컴퓨터와 코딩  전자책 시연 페이지 1','2026-07-13 10:35:45',NULL),(231,1151,2,'(쉽고 재밌는) 컴퓨터와 코딩  전자책 시연 페이지 2','2026-07-13 10:35:45',NULL),(232,1151,3,'(쉽고 재밌는) 컴퓨터와 코딩  전자책 시연 페이지 3','2026-07-13 10:35:45',NULL);
/*!40000 ALTER TABLE `book_page` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_rental`
--

DROP TABLE IF EXISTS `book_rental`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_rental` (
  `rental_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `book_id` bigint unsigned NOT NULL,
  `rental_start_at` datetime DEFAULT NULL,
  `rental_end_at` datetime NOT NULL,
  `rental_status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`rental_id`),
  KEY `idx_book_rental_member_id` (`member_id`),
  KEY `idx_book_rental_book_id` (`book_id`),
  KEY `idx_book_rental_rental_status` (`rental_status`),
  KEY `idx_book_rental_rental_end_at` (`rental_end_at`),
  CONSTRAINT `fk_book_rental_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_book_rental_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_book_rental_rental_status` CHECK ((`rental_status` in (_utf8mb4'READY',_utf8mb4'READING',_utf8mb4'OWNED')))
) ENGINE=InnoDB AUTO_INCREMENT=427 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_rental`
--

LOCK TABLES `book_rental` WRITE;
/*!40000 ALTER TABLE `book_rental` DISABLE KEYS */;
/*!40000 ALTER TABLE `book_rental` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_report`
--

DROP TABLE IF EXISTS `book_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_report` (
  `book_report_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint unsigned NOT NULL,
  `book_id` bigint unsigned NOT NULL,
  `report_type` varchar(30) NOT NULL,
  `content` text NOT NULL,
  `status` varchar(20) NOT NULL,
  `processed_at` datetime DEFAULT NULL,
  `process_result` text,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`book_report_id`),
  KEY `idx_book_report_reporter_id` (`reporter_id`),
  KEY `idx_book_report_book_id` (`book_id`),
  KEY `idx_book_report_status` (`status`),
  KEY `idx_book_report_created_at` (`created_at`),
  CONSTRAINT `fk_book_report_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_book_report_member` FOREIGN KEY (`reporter_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_book_report_status` CHECK ((`status` in (_utf8mb4'WAITING',_utf8mb4'PROCESSING',_utf8mb4'DONE',_utf8mb4'REJECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_report`
--

LOCK TABLES `book_report` WRITE;
/*!40000 ALTER TABLE `book_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `book_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_request`
--

DROP TABLE IF EXISTS `book_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_request` (
  `request_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `request_candidate_id` bigint unsigned NOT NULL,
  `member_id` bigint unsigned NOT NULL,
  `approved_book_id` bigint unsigned DEFAULT NULL,
  `request_title` varchar(255) NOT NULL,
  `request_author` varchar(100) NOT NULL,
  `request_publisher` varchar(100) NOT NULL,
  `normalized_title` varchar(255) NOT NULL,
  `normalized_author` varchar(100) NOT NULL,
  `normalized_publisher` varchar(100) NOT NULL,
  `reason` text NOT NULL,
  `request_status` varchar(20) NOT NULL,
  `processed_at` datetime DEFAULT NULL,
  `reject_reason` text,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`request_id`),
  UNIQUE KEY `uk_book_request_member_normalized` (`member_id`,`normalized_title`,`normalized_author`,`normalized_publisher`),
  KEY `fk_book_request_book` (`approved_book_id`),
  KEY `idx_book_request_request_candidate_id` (`request_candidate_id`),
  KEY `idx_book_request_request_status` (`request_status`),
  KEY `idx_book_request_created_at` (`created_at`),
  CONSTRAINT `fk_book_request_book` FOREIGN KEY (`approved_book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_book_request_book_request_candidate` FOREIGN KEY (`request_candidate_id`) REFERENCES `book_request_candidate` (`request_candidate_id`),
  CONSTRAINT `fk_book_request_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_book_request_request_status` CHECK ((`request_status` in (_utf8mb4'REQUESTED',_utf8mb4'APPROVED',_utf8mb4'REJECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_request`
--

LOCK TABLES `book_request` WRITE;
/*!40000 ALTER TABLE `book_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `book_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `book_request_candidate`
--

DROP TABLE IF EXISTS `book_request_candidate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_request_candidate` (
  `request_candidate_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `normalized_title` varchar(255) NOT NULL,
  `normalized_author` varchar(100) NOT NULL,
  `normalized_publisher` varchar(100) NOT NULL,
  `request_count` int unsigned NOT NULL DEFAULT '0',
  `first_requested_at` datetime NOT NULL,
  `candidate_status` varchar(20) NOT NULL,
  PRIMARY KEY (`request_candidate_id`),
  KEY `idx_book_request_candidate_normalized` (`normalized_title`,`normalized_author`,`normalized_publisher`),
  KEY `idx_book_request_candidate_candidate_status` (`candidate_status`),
  CONSTRAINT `chk_book_request_candidate_candidate_status` CHECK ((`candidate_status` in (_utf8mb4'REQUESTED',_utf8mb4'IN_REVIEW',_utf8mb4'APPROVED',_utf8mb4'REJECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `book_request_candidate`
--

LOCK TABLES `book_request_candidate` WRITE;
/*!40000 ALTER TABLE `book_request_candidate` DISABLE KEYS */;
/*!40000 ALTER TABLE `book_request_candidate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bookmark`
--

DROP TABLE IF EXISTS `bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookmark` (
  `bookmark_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `rental_id` bigint unsigned NOT NULL,
  `page_no` int unsigned NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`bookmark_id`),
  UNIQUE KEY `uk_bookmark_rental_id_page_no` (`rental_id`,`page_no`),
  CONSTRAINT `fk_bookmark_book_rental` FOREIGN KEY (`rental_id`) REFERENCES `book_rental` (`rental_id`)
) ENGINE=InnoDB AUTO_INCREMENT=154 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bookmark`
--

LOCK TABLES `bookmark` WRITE;
/*!40000 ALTER TABLE `bookmark` DISABLE KEYS */;
/*!40000 ALTER TABLE `bookmark` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `category_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `parent_category_id` bigint unsigned DEFAULT NULL,
  `category_name` varchar(100) NOT NULL,
  `display_order` int unsigned NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL,
  `aladin_category_id` varchar(20) DEFAULT NULL,
  `aladin_category_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_category_aladin_category_id` (`aladin_category_id`),
  UNIQUE KEY `uk_category_parent_name` (`parent_category_id`,`category_name`),
  CONSTRAINT `fk_category_category` FOREIGN KEY (`parent_category_id`) REFERENCES `category` (`category_id`),
  CONSTRAINT `chk_category_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=498 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (15,NULL,'국내도서',0,'ACTIVE',NULL,NULL),(16,15,'컴퓨터/모바일',0,'ACTIVE',NULL,NULL),(17,16,'컴퓨터 공학',0,'ACTIVE',NULL,NULL),(18,17,'소프트웨어 공학',0,'ACTIVE','2724','국내도서>컴퓨터/모바일>컴퓨터 공학>소프트웨어 공학'),(19,16,'프로그래밍',0,'ACTIVE',NULL,NULL),(158,15,'소설/시/희곡',0,'ACTIVE',NULL,NULL),(159,158,'한국소설',0,'ACTIVE',NULL,NULL),(160,159,'2000년대 이후 한국소설',0,'ACTIVE','50993','국내도서>소설/시/희곡>한국소설>2000년대 이후 한국소설'),(462,15,'어린이',0,'ACTIVE',NULL,NULL),(463,462,'과학/수학/컴퓨터',0,'ACTIVE',NULL,NULL),(464,463,'컴퓨터와 코딩',0,'ACTIVE','128216','국내도서>어린이>과학/수학/컴퓨터>컴퓨터와 코딩'),(465,463,'과학 일반',0,'ACTIVE','48867','국내도서>어린이>과학/수학/컴퓨터>과학 일반'),(466,462,'동화/명작/고전',0,'ACTIVE',NULL,NULL),(467,466,'국내창작동화',0,'ACTIVE','48874','국내도서>어린이>동화/명작/고전>국내창작동화'),(468,462,'초등 전학년',0,'ACTIVE',NULL,NULL),(469,468,'학습만화',0,'ACTIVE','49224','국내도서>어린이>초등 전학년>학습만화'),(470,463,'우리 몸',0,'ACTIVE','49609','국내도서>어린이>과학/수학/컴퓨터>우리 몸'),(471,15,'유아',0,'ACTIVE',NULL,NULL),(472,471,'그림책',0,'ACTIVE',NULL,NULL),(473,472,'나라별 그림책',0,'ACTIVE',NULL,NULL),(474,473,'외국 그림책',0,'ACTIVE','35140','국내도서>유아>그림책>나라별 그림책>외국 그림책'),(475,15,'청소년',0,'ACTIVE',NULL,NULL),(476,475,'청소년 문학',0,'ACTIVE',NULL,NULL),(477,476,'청소년 소설',0,'ACTIVE','50759','국내도서>청소년>청소년 문학>청소년 소설'),(478,15,'과학',0,'ACTIVE',NULL,NULL),(479,478,'물리학',0,'ACTIVE',NULL,NULL),(480,479,'양자역학',0,'ACTIVE','51296','국내도서>과학>물리학>양자역학'),(481,15,'자기계발',0,'ACTIVE',NULL,NULL),(482,481,'성공',0,'ACTIVE',NULL,NULL),(483,482,'성공학',0,'ACTIVE','70216','국내도서>자기계발>성공>성공학'),(484,478,'기초과학/교양과학',0,'ACTIVE','51002','국내도서>과학>기초과학/교양과학'),(485,15,'사회과학',0,'ACTIVE',NULL,NULL),(486,485,'미래학',0,'ACTIVE','51008','국내도서>사회과학>미래학'),(487,15,'건강/취미',0,'ACTIVE',NULL,NULL),(488,487,'질병치료와 예방',0,'ACTIVE',NULL,NULL),(489,488,'허리/디스크/척추/통증',0,'ACTIVE','53550','국내도서>건강/취미>질병치료와 예방>허리/디스크/척추/통증'),(490,462,'문화/예술/인물',0,'ACTIVE',NULL,NULL),(491,490,'한국인물',0,'ACTIVE','48902','국내도서>어린이>문화/예술/인물>한국인물'),(492,466,'외국창작동화',0,'ACTIVE','48877','국내도서>어린이>동화/명작/고전>외국창작동화'),(493,462,'학습만화',0,'ACTIVE','112080','국내도서>어린이>학습만화'),(494,15,'경제경영',0,'ACTIVE',NULL,NULL),(495,494,'경제학/경제일반',0,'ACTIVE',NULL,NULL),(496,495,'경제이야기',0,'ACTIVE','3065','국내도서>경제경영>경제학/경제일반>경제이야기'),(497,463,'초등 수학',0,'ACTIVE','48871','국내도서>어린이>과학/수학/컴퓨터>초등 수학');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `challenge`
--

DROP TABLE IF EXISTS `challenge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `challenge` (
  `challenge_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `challenge_name` varchar(100) NOT NULL,
  `challenge_type` varchar(20) NOT NULL,
  `target_action` varchar(50) NOT NULL,
  `target_count` int unsigned NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`challenge_id`),
  CONSTRAINT `chk_challenge_challenge_type` CHECK ((`challenge_type` in (_utf8mb4'DAILY',_utf8mb4'TOTAL'))),
  CONSTRAINT `chk_challenge_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=341 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `challenge`
--

LOCK TABLES `challenge` WRITE;
/*!40000 ALTER TABLE `challenge` DISABLE KEYS */;
/*!40000 ALTER TABLE `challenge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `challenge_progress`
--

DROP TABLE IF EXISTS `challenge_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `challenge_progress` (
  `challenge_progress_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `challenge_id` bigint unsigned NOT NULL,
  `progress_date` date NOT NULL,
  `current_count` int unsigned NOT NULL DEFAULT '0',
  `achieved_yn` tinyint(1) NOT NULL DEFAULT '0',
  `achieved_at` datetime DEFAULT NULL,
  `reward_claimed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`challenge_progress_id`),
  UNIQUE KEY `uk_challenge_progress_member_id_challenge_id_progress_date` (`member_id`,`challenge_id`,`progress_date`),
  KEY `fk_challenge_progress_challenge` (`challenge_id`),
  CONSTRAINT `fk_challenge_progress_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`challenge_id`),
  CONSTRAINT `fk_challenge_progress_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=188 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `challenge_progress`
--

LOCK TABLES `challenge_progress` WRITE;
/*!40000 ALTER TABLE `challenge_progress` DISABLE KEYS */;
/*!40000 ALTER TABLE `challenge_progress` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `challenge_reward`
--

DROP TABLE IF EXISTS `challenge_reward`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `challenge_reward` (
  `challenge_reward_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `challenge_id` bigint unsigned NOT NULL,
  `reward_type` varchar(20) NOT NULL,
  `reward_point` int DEFAULT NULL,
  `coupon_id` bigint unsigned DEFAULT NULL,
  `reward_quantity` int unsigned NOT NULL DEFAULT '1',
  PRIMARY KEY (`challenge_reward_id`),
  KEY `fk_challenge_reward_challenge` (`challenge_id`),
  KEY `fk_challenge_reward_coupon` (`coupon_id`),
  CONSTRAINT `fk_challenge_reward_challenge` FOREIGN KEY (`challenge_id`) REFERENCES `challenge` (`challenge_id`),
  CONSTRAINT `fk_challenge_reward_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`coupon_id`),
  CONSTRAINT `chk_challenge_reward_reward_type` CHECK ((`reward_type` in (_utf8mb4'POINT',_utf8mb4'coupon')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `challenge_reward`
--

LOCK TABLES `challenge_reward` WRITE;
/*!40000 ALTER TABLE `challenge_reward` DISABLE KEYS */;
/*!40000 ALTER TABLE `challenge_reward` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collection`
--

DROP TABLE IF EXISTS `collection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collection` (
  `collection_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `created_by` bigint unsigned DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `collection_type` varchar(20) NOT NULL,
  `discount_rate` decimal(5,2) DEFAULT NULL,
  `description` text,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`collection_id`),
  KEY `idx_collection_collection_type` (`collection_type`),
  KEY `idx_collection_status` (`status`),
  KEY `idx_collection_start_date` (`start_date`),
  KEY `idx_collection_end_date` (`end_date`),
  KEY `idx_collection_created_by` (`created_by`),
  CONSTRAINT `fk_collection_member` FOREIGN KEY (`created_by`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_collection_collection_type` CHECK ((`collection_type` in (_utf8mb4'SERIES',_utf8mb4'EVENT'))),
  CONSTRAINT `chk_collection_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'HIDDEN',_utf8mb4'ENDED')))
) ENGINE=InnoDB AUTO_INCREMENT=195 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collection`
--

LOCK TABLES `collection` WRITE;
/*!40000 ALTER TABLE `collection` DISABLE KEYS */;
/*!40000 ALTER TABLE `collection` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collection_book`
--

DROP TABLE IF EXISTS `collection_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collection_book` (
  `collection_book_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `collection_id` bigint unsigned NOT NULL,
  `book_id` bigint unsigned NOT NULL,
  `display_order` int unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`collection_book_id`),
  UNIQUE KEY `uk_collection_book_collection_id_book_id` (`collection_id`,`book_id`),
  KEY `idx_collection_book_book_id` (`book_id`),
  KEY `idx_collection_book_display_order` (`display_order`),
  CONSTRAINT `fk_collection_book_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_collection_book_collection` FOREIGN KEY (`collection_id`) REFERENCES `collection` (`collection_id`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collection_book`
--

LOCK TABLES `collection_book` WRITE;
/*!40000 ALTER TABLE `collection_book` DISABLE KEYS */;
/*!40000 ALTER TABLE `collection_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon`
--

DROP TABLE IF EXISTS `coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon` (
  `coupon_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `coupon_name` varchar(100) NOT NULL,
  `coupon_type` varchar(30) NOT NULL,
  `benefit_value` int NOT NULL DEFAULT '0',
  `benefit_unit` varchar(20) NOT NULL,
  `valid_days` int unsigned NOT NULL DEFAULT '14',
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`coupon_id`),
  CONSTRAINT `chk_coupon_benefit_unit` CHECK ((`benefit_unit` in (_utf8mb4'AMOUNT',_utf8mb4'PERCENT'))),
  CONSTRAINT `chk_coupon_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon`
--

LOCK TABLES `coupon` WRITE;
/*!40000 ALTER TABLE `coupon` DISABLE KEYS */;
/*!40000 ALTER TABLE `coupon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon_usage`
--

DROP TABLE IF EXISTS `coupon_usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_usage` (
  `coupon_usage_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_coupon_id` bigint unsigned NOT NULL,
  `rental_id` bigint unsigned DEFAULT NULL,
  `payment_id` bigint unsigned DEFAULT NULL,
  `discount_amount` int NOT NULL DEFAULT '0',
  `usage_note` varchar(255) DEFAULT NULL,
  `used_at` datetime NOT NULL,
  PRIMARY KEY (`coupon_usage_id`),
  UNIQUE KEY `uk_coupon_usage_member_coupon_id` (`member_coupon_id`),
  UNIQUE KEY `uk_coupon_usage_payment_id` (`payment_id`),
  KEY `idx_coupon_usage_rental_id` (`rental_id`),
  CONSTRAINT `fk_coupon_usage_book_rental` FOREIGN KEY (`rental_id`) REFERENCES `book_rental` (`rental_id`),
  CONSTRAINT `fk_coupon_usage_member_coupon` FOREIGN KEY (`member_coupon_id`) REFERENCES `member_coupon` (`member_coupon_id`),
  CONSTRAINT `fk_coupon_usage_payment` FOREIGN KEY (`payment_id`) REFERENCES `payment` (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon_usage`
--

LOCK TABLES `coupon_usage` WRITE;
/*!40000 ALTER TABLE `coupon_usage` DISABLE KEYS */;
/*!40000 ALTER TABLE `coupon_usage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorite_book`
--

DROP TABLE IF EXISTS `favorite_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite_book` (
  `favorite_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `book_id` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `uk_favorite_book_member_id_book_id` (`member_id`,`book_id`),
  KEY `fk_favorite_book_book` (`book_id`),
  CONSTRAINT `fk_favorite_book_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_favorite_book_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorite_book`
--

LOCK TABLES `favorite_book` WRITE;
/*!40000 ALTER TABLE `favorite_book` DISABLE KEYS */;
/*!40000 ALTER TABLE `favorite_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `library`
--

DROP TABLE IF EXISTS `library`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `library` (
  `library_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `library_name` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`library_id`),
  CONSTRAINT `chk_library_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=154 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `library`
--

LOCK TABLES `library` WRITE;
/*!40000 ALTER TABLE `library` DISABLE KEYS */;
/*!40000 ALTER TABLE `library` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `library_book`
--

DROP TABLE IF EXISTS `library_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `library_book` (
  `library_book_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `library_id` bigint unsigned NOT NULL,
  `book_id` bigint unsigned NOT NULL,
  `holding_status` varchar(20) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`library_book_id`),
  UNIQUE KEY `uk_library_book_library_id_book_id` (`library_id`,`book_id`),
  KEY `idx_library_book_book_id` (`book_id`),
  KEY `idx_library_book_holding_status` (`holding_status`),
  CONSTRAINT `fk_library_book_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_library_book_library` FOREIGN KEY (`library_id`) REFERENCES `library` (`library_id`),
  CONSTRAINT `chk_library_book_holding_status` CHECK ((`holding_status` in (_utf8mb4'AVAILABLE',_utf8mb4'UNAVAILABLE')))
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `library_book`
--

LOCK TABLES `library_book` WRITE;
/*!40000 ALTER TABLE `library_book` DISABLE KEYS */;
/*!40000 ALTER TABLE `library_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `member_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `login_id` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(50) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `birth_date` date NOT NULL,
  `gender` varchar(20) DEFAULT NULL,
  `role` varchar(20) NOT NULL,
  `grade_id` bigint unsigned NOT NULL,
  `grade_expires_at` datetime DEFAULT NULL,
  `grade_payment_started_at` datetime DEFAULT NULL,
  `point_balance` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL,
  `withdrawn_at` datetime DEFAULT NULL,
  `deactivated_at` datetime DEFAULT NULL,
  `anonymized_at` datetime DEFAULT NULL,
  `withdrawal_reason` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_member_login_id` (`login_id`),
  UNIQUE KEY `uk_member_email` (`email`),
  KEY `idx_member_grade_id` (`grade_id`),
  KEY `idx_member_status` (`status`),
  CONSTRAINT `fk_member_member_grade` FOREIGN KEY (`grade_id`) REFERENCES `member_grade` (`grade_id`),
  CONSTRAINT `chk_member_point_balance` CHECK ((`point_balance` >= 0)),
  CONSTRAINT `chk_member_role` CHECK ((`role` in (_utf8mb4'USER',_utf8mb4'ADMIN'))),
  CONSTRAINT `chk_member_status` CHECK ((`status` in (_utf8mb4'JOINED',_utf8mb4'SANCTIONED',_utf8mb4'WITHDRAWN',_utf8mb4'DEACTIVATED')))
) ENGINE=InnoDB AUTO_INCREMENT=787 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member`
--

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;
INSERT INTO `member` VALUES (1,'admin','$2a$10$jWgn1R7YXtIpHcUUzcviBe1q5a0PDdGawnXIZOhS5pzvFetVJwY7u','홍길동','Admin@admin.com','010-1234-5678','1995-06-15','MALE','ADMIN',1,NULL,NULL,0,'JOINED',NULL,NULL,NULL,NULL,'2026-07-11 10:34:47','2026-07-11 10:34:47');
/*!40000 ALTER TABLE `member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_coupon`
--

DROP TABLE IF EXISTS `member_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_coupon` (
  `member_coupon_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `coupon_id` bigint unsigned NOT NULL,
  `coupon_status` varchar(20) NOT NULL,
  `issued_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  `used_at` datetime DEFAULT NULL,
  PRIMARY KEY (`member_coupon_id`),
  KEY `fk_member_coupon_coupon` (`coupon_id`),
  KEY `idx_member_coupon_member_id` (`member_id`),
  KEY `idx_member_coupon_coupon_status` (`coupon_status`),
  KEY `idx_member_coupon_expires_at` (`expires_at`),
  CONSTRAINT `fk_member_coupon_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`coupon_id`),
  CONSTRAINT `fk_member_coupon_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_member_coupon_coupon_status` CHECK ((`coupon_status` in (_utf8mb4'ISSUED',_utf8mb4'USED',_utf8mb4'EXPIRED')))
) ENGINE=InnoDB AUTO_INCREMENT=256 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_coupon`
--

LOCK TABLES `member_coupon` WRITE;
/*!40000 ALTER TABLE `member_coupon` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_coupon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_grade`
--

DROP TABLE IF EXISTS `member_grade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_grade` (
  `grade_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `grade_name` varchar(50) NOT NULL,
  `grade_level` int NOT NULL,
  `min_payment_amount` int NOT NULL DEFAULT '0',
  `grade_period_months` int unsigned NOT NULL,
  `maintain_payment_rate` decimal(5,2) NOT NULL DEFAULT '10.00',
  `description` text,
  PRIMARY KEY (`grade_id`),
  UNIQUE KEY `uk_member_grade_grade_name` (`grade_name`),
  UNIQUE KEY `uk_member_grade_grade_level` (`grade_level`)
) ENGINE=InnoDB AUTO_INCREMENT=111 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_grade`
--

LOCK TABLES `member_grade` WRITE;
/*!40000 ALTER TABLE `member_grade` DISABLE KEYS */;
INSERT INTO `member_grade` VALUES (1,'씨앗',1,0,0,10.00,'기본 등급. 가입 즉시 부여되며 유지기간 없음.'),(2,'새싹',2,20000,1,10.00,'1개월 누적 결제 2만 원 이상 달성 시 승급.'),(3,'나무',3,100000,3,10.00,'3개월 누적 결제 10만 원 이상 달성 시 승급.'),(4,'도토리',4,700000,6,10.00,'6개월 누적 결제 70만 원 이상 달성 시 승급.');
/*!40000 ALTER TABLE `member_grade` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `notice_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `notice_status` varchar(20) NOT NULL,
  `notify_yn` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`notice_id`),
  CONSTRAINT `chk_notice_notice_status` CHECK ((`notice_status` in (_utf8mb4'ACTIVE',_utf8mb4'HIDDEN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `notification_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `notification_type` varchar(30) NOT NULL,
  `title` varchar(100) NOT NULL,
  `target_type` varchar(30) DEFAULT NULL,
  `target_id` bigint unsigned DEFAULT NULL,
  `read_yn` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `read_at` datetime DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notification_member_id` (`member_id`),
  KEY `idx_notification_read_yn` (`read_yn`),
  KEY `idx_notification_created_at` (`created_at`),
  KEY `idx_notification_notification_type` (`notification_type`),
  CONSTRAINT `fk_notification_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_notification_notification_type` CHECK ((`notification_type` in (_utf8mb4'RENTAL',_utf8mb4'payment',_utf8mb4'notice',_utf8mb4'EVENT',_utf8mb4'REPORT',_utf8mb4'book_request',_utf8mb4'coupon',_utf8mb4'challenge')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_code`
--

DROP TABLE IF EXISTS `password_reset_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_code` (
  `password_reset_code_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `email` varchar(255) NOT NULL,
  `code_hash` varchar(255) NOT NULL,
  `status` varchar(20) NOT NULL,
  `fail_count` int unsigned NOT NULL DEFAULT '0',
  `expires_at` datetime NOT NULL,
  `verified_at` datetime DEFAULT NULL,
  `used_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`password_reset_code_id`),
  KEY `idx_password_reset_code_member_id_status_expires_at` (`member_id`,`status`,`expires_at`),
  KEY `idx_password_reset_code_member_id_created_at` (`member_id`,`created_at`),
  KEY `idx_password_reset_code_status_created_at` (`status`,`created_at`),
  KEY `idx_password_reset_code_created_at` (`created_at`),
  CONSTRAINT `fk_password_reset_code_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_password_reset_code_status` CHECK ((`status` in (_utf8mb4'ISSUED',_utf8mb4'VERIFIED',_utf8mb4'USED',_utf8mb4'EXPIRED',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_code`
--

LOCK TABLES `password_reset_code` WRITE;
/*!40000 ALTER TABLE `password_reset_code` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_code` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `payment_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `rental_id` bigint unsigned NOT NULL,
  `payment_type` varchar(20) NOT NULL,
  `original_amount` int NOT NULL DEFAULT '0',
  `amount` int NOT NULL DEFAULT '0',
  `payment_method` varchar(20) NOT NULL,
  `payment_status` varchar(20) NOT NULL,
  `paid_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `idx_payment_member_id` (`member_id`),
  KEY `idx_payment_rental_id` (`rental_id`),
  KEY `idx_payment_payment_status` (`payment_status`),
  KEY `idx_payment_paid_at` (`paid_at`),
  CONSTRAINT `fk_payment_book_rental` FOREIGN KEY (`rental_id`) REFERENCES `book_rental` (`rental_id`),
  CONSTRAINT `fk_payment_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_payment_amount` CHECK ((`amount` >= 0)),
  CONSTRAINT `chk_payment_payment_method` CHECK ((`payment_method` = _utf8mb4'CARD')),
  CONSTRAINT `chk_payment_payment_status` CHECK ((`payment_status` = _utf8mb4'PAID')),
  CONSTRAINT `chk_payment_payment_type` CHECK ((`payment_type` = _utf8mb4'RENTAL'))
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `point_history`
--

DROP TABLE IF EXISTS `point_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `point_history` (
  `point_history_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `payment_id` bigint unsigned DEFAULT NULL,
  `point_type` varchar(30) NOT NULL,
  `point_amount` int NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`point_history_id`),
  KEY `idx_point_history_member_id` (`member_id`),
  KEY `idx_point_history_payment_id` (`payment_id`),
  KEY `idx_point_history_point_type` (`point_type`),
  KEY `idx_point_history_created_at` (`created_at`),
  CONSTRAINT `fk_point_history_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_point_history_payment` FOREIGN KEY (`payment_id`) REFERENCES `payment` (`payment_id`),
  CONSTRAINT `chk_point_history_point_type` CHECK ((`point_type` in (_utf8mb4'challenge_reward',_utf8mb4'REVIEW_REWARD',_utf8mb4'PAYBACK',_utf8mb4'EVENT_REWARD',_utf8mb4'PAYMENT_USE',_utf8mb4'ADMIN_ADJUST')))
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `point_history`
--

LOCK TABLES `point_history` WRITE;
/*!40000 ALTER TABLE `point_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `point_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `publisher`
--

DROP TABLE IF EXISTS `publisher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `publisher` (
  `publisher_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `publisher_name` varchar(100) NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`publisher_id`),
  UNIQUE KEY `uk_publisher_publisher_name` (`publisher_name`),
  KEY `idx_publisher_status` (`status`),
  CONSTRAINT `chk_publisher_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE')))
) ENGINE=InnoDB AUTO_INCREMENT=321 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `publisher`
--

LOCK TABLES `publisher` WRITE;
/*!40000 ALTER TABLE `publisher` DISABLE KEYS */;
INSERT INTO `publisher` VALUES (5,'인사이트','ACTIVE','2026-07-11 10:42:27'),(6,'테스트출판사','ACTIVE','2026-07-11 15:25:46'),(97,'창비','ACTIVE','2026-07-11 16:40:57'),(295,'위즈덤하우스','ACTIVE','2026-07-13 10:26:32'),(296,'예림당','ACTIVE','2026-07-13 10:26:32'),(297,'낮은산','ACTIVE','2026-07-13 10:34:05'),(298,'보리','ACTIVE','2026-07-13 10:34:05'),(299,'좋은책신사고 좋은책어린이','ACTIVE','2026-07-13 10:34:05'),(300,'서울문화사','ACTIVE','2026-07-13 10:34:05'),(301,'비룡소','ACTIVE','2026-07-13 10:34:05'),(302,'Usborne Korea','ACTIVE','2026-07-13 10:34:05'),(303,'미래엔','ACTIVE','2026-07-13 10:34:05'),(304,'뜨인돌','ACTIVE','2026-07-13 10:34:05'),(305,'사이언스북스','ACTIVE','2026-07-13 10:34:05'),(306,'스튜디오다산','ACTIVE','2026-07-13 10:34:06'),(307,'김영사','ACTIVE','2026-07-13 10:34:06'),(308,'와이즈만 Books','ACTIVE','2026-07-13 10:34:06'),(309,'뿌리와이파리','ACTIVE','2026-07-13 10:34:06'),(310,'우리교육','ACTIVE','2026-07-13 10:34:06'),(311,'웅진씽크빅','ACTIVE','2026-07-13 10:34:06'),(312,'스튜디오 다산','ACTIVE','2026-07-13 10:34:06'),(313,'고릴라박스','ACTIVE','2026-07-13 10:34:06'),(314,'글송이','ACTIVE','2026-07-13 10:34:06'),(315,'상상아카데미','ACTIVE','2026-07-13 10:35:45'),(316,'청림출판','ACTIVE','2026-07-13 10:35:45'),(317,'어스본코리아','ACTIVE','2026-07-13 10:35:45'),(318,'푸른숲','ACTIVE','2026-07-13 10:35:45'),(319,'길벗','ACTIVE','2026-07-13 10:35:45'),(320,'대원씨아이','ACTIVE','2026-07-13 10:35:45');
/*!40000 ALTER TABLE `publisher` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rating`
--

DROP TABLE IF EXISTS `rating`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rating` (
  `rating_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `book_id` bigint unsigned NOT NULL,
  `rating_score` tinyint unsigned NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`rating_id`),
  UNIQUE KEY `uk_rating_member_id_book_id` (`member_id`,`book_id`),
  KEY `idx_rating_book_id` (`book_id`),
  CONSTRAINT `fk_rating_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_rating_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_rating_rating_score` CHECK ((`rating_score` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=169 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rating`
--

LOCK TABLES `rating` WRITE;
/*!40000 ALTER TABLE `rating` DISABLE KEYS */;
/*!40000 ALTER TABLE `rating` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reading_progress`
--

DROP TABLE IF EXISTS `reading_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reading_progress` (
  `progress_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `rental_id` bigint unsigned NOT NULL,
  `current_page` int unsigned NOT NULL DEFAULT '1',
  `read_page_count` int unsigned NOT NULL DEFAULT '0',
  `last_read_at` datetime DEFAULT NULL,
  `completed_yn` tinyint(1) NOT NULL DEFAULT '0',
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`progress_id`),
  UNIQUE KEY `uk_reading_progress_rental_id` (`rental_id`),
  KEY `idx_reading_progress_completed_yn` (`completed_yn`),
  CONSTRAINT `fk_reading_progress_book_rental` FOREIGN KEY (`rental_id`) REFERENCES `book_rental` (`rental_id`)
) ENGINE=InnoDB AUTO_INCREMENT=229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reading_progress`
--

LOCK TABLES `reading_progress` WRITE;
/*!40000 ALTER TABLE `reading_progress` DISABLE KEYS */;
/*!40000 ALTER TABLE `reading_progress` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `review_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `book_id` bigint unsigned NOT NULL,
  `content` text NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`review_id`),
  UNIQUE KEY `uk_review_member_id_book_id` (`member_id`,`book_id`),
  KEY `idx_review_book_id` (`book_id`),
  KEY `idx_review_status` (`status`),
  CONSTRAINT `fk_review_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`book_id`),
  CONSTRAINT `fk_review_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_review_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'HIDDEN',_utf8mb4'DELETED')))
) ENGINE=InnoDB AUTO_INCREMENT=177 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_report`
--

DROP TABLE IF EXISTS `review_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_report` (
  `review_report_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint unsigned NOT NULL,
  `review_id` bigint unsigned NOT NULL,
  `report_type` varchar(30) NOT NULL,
  `content` text NOT NULL,
  `status` varchar(20) NOT NULL,
  `processed_at` datetime DEFAULT NULL,
  `process_result` text,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`review_report_id`),
  KEY `idx_review_report_reporter_id` (`reporter_id`),
  KEY `idx_review_report_review_id` (`review_id`),
  KEY `idx_review_report_status` (`status`),
  KEY `idx_review_report_created_at` (`created_at`),
  CONSTRAINT `fk_review_report_member` FOREIGN KEY (`reporter_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_review_report_review` FOREIGN KEY (`review_id`) REFERENCES `review` (`review_id`),
  CONSTRAINT `chk_review_report_status` CHECK ((`status` in (_utf8mb4'WAITING',_utf8mb4'PROCESSING',_utf8mb4'DONE',_utf8mb4'REJECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_report`
--

LOCK TABLES `review_report` WRITE;
/*!40000 ALTER TABLE `review_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sanction_history`
--

DROP TABLE IF EXISTS `sanction_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sanction_history` (
  `sanction_history_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `review_report_id` bigint unsigned DEFAULT NULL,
  `sanction_type` varchar(30) NOT NULL,
  `reason` text,
  `started_at` datetime NOT NULL,
  `ended_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`sanction_history_id`),
  KEY `idx_sanction_history_member_id` (`member_id`),
  KEY `idx_sanction_history_review_report_id` (`review_report_id`),
  KEY `idx_sanction_history_started_at` (`started_at`),
  KEY `idx_sanction_history_ended_at` (`ended_at`),
  CONSTRAINT `fk_sanction_history_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_sanction_history_review_report` FOREIGN KEY (`review_report_id`) REFERENCES `review_report` (`review_report_id`),
  CONSTRAINT `chk_sanction_history_sanction_type` CHECK ((`sanction_type` in (_utf8mb4'RENTAL_BAN',_utf8mb4'REVIEW_BAN',_utf8mb4'ACCOUNT_SUSPENSION')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sanction_history`
--

LOCK TABLES `sanction_history` WRITE;
/*!40000 ALTER TABLE `sanction_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `sanction_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_reading_statistic`
--

DROP TABLE IF EXISTS `user_reading_statistic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_reading_statistic` (
  `user_stat_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `member_id` bigint unsigned NOT NULL,
  `period_type` varchar(20) NOT NULL,
  `period_start_date` date NOT NULL,
  `period_end_date` date NOT NULL,
  `rental_count` int unsigned NOT NULL DEFAULT '0',
  `read_book_count` int unsigned NOT NULL DEFAULT '0',
  `read_page_count` int unsigned NOT NULL DEFAULT '0',
  `review_count` int unsigned NOT NULL DEFAULT '0',
  `favorite_count` int unsigned NOT NULL DEFAULT '0',
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`user_stat_id`),
  UNIQUE KEY `uk_user_reading_stat_member_period_start` (`member_id`,`period_type`,`period_start_date`),
  CONSTRAINT `fk_user_reading_statistic_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `chk_user_reading_statistic_period_type` CHECK ((`period_type` in (_utf8mb4'WEEKLY',_utf8mb4'MONTHLY',_utf8mb4'YEARLY',_utf8mb4'TOTAL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_reading_statistic`
--

LOCK TABLES `user_reading_statistic` WRITE;
/*!40000 ALTER TABLE `user_reading_statistic` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_reading_statistic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'abc'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-13 10:41:43
