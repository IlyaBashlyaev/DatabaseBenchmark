# DB Benchmark

## Definition
A Java project for benchmarking the HSQLDB and PostgreSQL databases. This project is part of the Blockwoche 2026 at Robert-Bosch-Berufskolleg Dortmund.

---

## Setup

### Clone
```
git clone https://github.com/IlyaBashlyaev/DatabaseBenchmark.git
```

### Source code
1. Download [Apache NetBeans](https://netbeans.apache.org/front/main/download/).
2. Move the files to the `~/NetBeansProjects` directory.
3. Open the project (`Ctrl + O`) and select the root directory.

### HSQLDB
1. Open a terminal in the root directory:
```
cd ~/NetBeansProjects/DatabaseBenchmark
```
2. Run the HSQLDB jar:
```
java -jar hsqldb.jar
```
3. Connect with the following settings:
```
Type:     HSQL Database Engine In-Memory
Driver:   org.hsqldb.jdbc.JDBCDriver
URL:      jdbc:hsqldb:file:./data/onlineshop
User:     SA
Password:
```

---

### PostgreSQL

#### Windows 11

1. Download the PostgreSQL installer from the [official website](https://www.postgresql.org/download/windows/) and run it.
2. During installation set:
   - **Password** for the `postgres` superuser: `postgres`
   - **Port**: `5432` (default)
3. After installation open **pgAdmin** or **SQL Shell (psql)** from the Start menu.
4. Connect as `postgres` and create the database:
```sql
CREATE DATABASE onlineshop;
```
5. Make sure the PostgreSQL bin directory is added to your `PATH`
   (the installer offers this option automatically).

#### Unix (macOS / Linux)

##### macOS
1. Install PostgreSQL via [Homebrew](https://brew.sh):
```
brew install postgresql@17
```
2. Start the PostgreSQL service:
```
brew services start postgresql@17
```

##### Linux
1. Install PostgreSQL via apt:
```
sudo apt update
sudo apt install postgresql postgresql-contrib
```
2. Switch to PostgreSQL system user:
```
sudo -u postgres psql
```

##### Unix
3. Open the PostgreSQL shell:
```
psql postgres
```
4. Create the database and user:
```sql
CREATE DATABASE onlineshop;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE onlineshop TO postgres;
```
5. Exit the shell:
```
\q
```

#### Verify the connection (both platforms)

The benchmark connects to PostgreSQL with these default credentials:
```
Host:     localhost
Database: onlineshop
User:     postgres
Password: postgres
```