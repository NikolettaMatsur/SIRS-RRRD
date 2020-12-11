-- MySQL dump 10.13  Distrib 8.0.22, for Win64 (x86_64)
--
-- Host: localhost    Database: rrrd
-- ------------------------------------------------------
-- Server version	8.0.22

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
-- Current Database: `rrrd`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `rrrd` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `rrrd`;

--
-- Table structure for table `files`
--

DROP TABLE IF EXISTS `files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `files` (
  `filename` varchar(500) NOT NULL,
  `owner` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `files`
--

LOCK TABLES `files` WRITE;
/*!40000 ALTER TABLE `files` DISABLE KEYS */;
/*!40000 ALTER TABLE `files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `file_id` int NOT NULL,
  `username` varchar(30) NOT NULL,
  `pub_key_id` int NOT NULL,
  `permission_key` varchar(4096) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `public_keys`
--

DROP TABLE IF EXISTS `public_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_keys` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(30) NOT NULL,
  `pub_key` varchar(2048) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `public_keys`
--

LOCK TABLES `public_keys` WRITE;
/*!40000 ALTER TABLE `public_keys` DISABLE KEYS */;
INSERT INTO `public_keys` VALUES (1,'client1','MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkA319IUdJ/IgBAxhJiqNt6p2t6yeIhFNw8sAuZNsPxXl5MksRJKjGUptdeXGrFvAWCvGW0GjH0MkwILEywwmoA74nC7OVGbGEUgYbTM1eK9EL4u/0/FpGXF1CanzvjDQCdE8mgAqJwcXyYsl85Zl32X8CUYTBkqqJAQf6Bp/m8DwZlHQghFEKDZmf1cP+Xcfvz3X1Ulax4B+rSVRz/C9E8NKHQnV8FGLa5LxfrnVw7dEpaONLv0cwf5w/paTUney7893yywvT5zWxbuEkO+SdzxcWk2ydyzUAgpSW/Rrg2yE6lBBGvzXROugpKGfcPgeUmJdHevSC6ClaAWFqaDquwIDAQAB'),(2,'client2','MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmSIrCajCO94g1T4Clx/A48wsz7sxBA2C86vzPNKkk8C1rzTE2K9wS2g9IpNEtUMWFZXaBAtbl10EJn6RMwCBw8cRpjaUSEuIhKkUVBY3q04ke2ldZexPYty/EaBATgg+hXb4nunS8Gb9IvziKLgbHcpjwckzV02EtEmX+juDb/iyKs0VwKwnOmuFsfmkW5EGphPjbBIdYy8Fcj67Mvwi2y75ZpvpXCava61/29ShmxDr0Wb448WWqQ4i9LqS/hVka38U6Zwmmd0SMYOZRcRZqsqi5DczVX7g4bsPC2GVKOjRKi76w1/sKx/Ap2L9MOBuRX7me+xvT6ccyjizw1PqewIDAQAB'),(3,'client3','MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm6TJBoUH0WxcmceWXieLgwSUWN5ft2hFWgCR5XM4py/DuprIe35zFiwEKAZqA9Trl3S7bhPHtpF2AhRDRtshFUZ/MlkE6Ftr8IBXd4fwScLMm/oIh8T2hRLQWM3zBtggL/gXvNoa2osGsP9dcAYexQWLZ8S8cEE5G6xYlH6ri0dFLAoCWs86DCEvmZz+mvU0ENWL/c+Q1he6bXjEMULpNr9Lbj2tLbKQ2X/zq/AJprdVK+c8fLARcJnzoMhmbIVdxz7oRPzsZPz79fFYogxqkHs8r8yXmk/3rSMG6u7WAZ92h96QP4apQiOaaZwtmFEkFWDAU4Vz8KRttWo8UJOYewIDAQAB');
/*!40000 ALTER TABLE `public_keys` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `username` varchar(30) NOT NULL,
  `password` varbinary(500) NOT NULL,
  `salt` varbinary(500) NOT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('client1',_binary 'dÃ ¤Py7ªyÇ\ñ',_binary '\ðü|¸%OÃ2YA?'),('client2',_binary '¶ª(¯BÓq\Ê\í\Ä\Ö\â',_binary 'ù:ÿ¬\ï\ô>\\\âs®\ãZ\Õ'),('client3',_binary '\ë³9½b\óG_1mrúj',_binary 'c\Å½íºG\áKM');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-12-11 22:56:33
