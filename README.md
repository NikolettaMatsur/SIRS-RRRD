Instituto Superior Técnico, Universidade de Lisboa

**Network and Computer Security**

#Project - Ransomware Resistant Remote Documents
##Group 44

## Required platform
The required platform to run this project is Linux 64-bit, Ubuntu 20.04.1 LTS, Java 11 and MySQL.

## Setup
**We refer to the directory SIRS-RRRD as <project-root>**

### Create the database
The system needs a database to run

A database can be created with MySQL [MySQL](https://www.mysql.com/)
To do so first you need to install MySQL by running:

```bash
$ sudo apt install mysql-server
```

To setup the databse:

```bash
$ sudo mysql -u root
```

```sql
mysql> CREATE DATABASE db;
```

```sql
mysql> CREATE USER '<username>'@'localhost' IDENTIFIED BY '<password>';
```

```sql
mysql> GRANT ALL PRIVILEGES ON db.* TO '<username>'@'localhost';
```
***
Populate the database:


### Create the necessary folder structure
create folders in backup****
create folders in client****

### Compile the code

The code can be compiled using [Maven](https://maven.apache.org/).
To do so at the root of the project <project-root> run:

```bash
$ mvn clean install
```

## Run Project
### Client
### Server
### Backup