DROP TABLE IF EXISTS users, public_keys, files, permissions;

CREATE TABLE IF NOT EXISTS users(username varchar(30) PRIMARY KEY, password varbinary(500) NOT NULL, salt varbinary(500) NOT NULL);
CREATE TABLE IF NOT EXISTS files(filename varchar(500) PRIMARY KEY, owner varchar(30));
CREATE TABLE IF NOT EXISTS public_keys(id int AUTO_INCREMENT PRIMARY KEY, username varchar(30) NOT NULL, pub_key varchar(2048) NOT NULL);
CREATE TABLE IF NOT EXISTS permissions(id int AUTO_INCREMENT PRIMARY KEY, filename varchar(500) NOT NULL,  username varchar(30) NOT NULL, pub_key_id int NOT NULL, permission_key varchar(4096) NOT NULL);