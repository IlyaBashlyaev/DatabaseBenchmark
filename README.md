<!-- TODO: Update README.md and translate it in English -->

# DB Benchmark

## Definition
Das Java-Projekt zum Benchmark der HSQLDB-Datenbank. Dieses Projekt gehört zur Blockwoche 2026 am Robert-Bosch-Berufskolleg Dortmund.

---

## Einrichtung

### Klonen
```
git clone https://github.com/IlyaBashlyaev/DatabaseBenchmark.git
```

### Quellcode
1) [Apache NetBeans](https://netbeans.apache.org/front/main/download/) herunterladen.
2) Dateien zum Verzeichnis "~/NetBeansProjects" verschieben.
2) Projekt öffnen (Strg + O) und Hauptverzeichnis auswählen.

### Datenbank
1) Terminal im Hauptverzeichnis öffnen:
```
cd ~/NetBeansProjects/DatabaseBenchmark
```
2) HSQLDB-Datei ausführen:
```
java -jar hsqldb.jar
```
3) Verbindung mit folgenden Daten herstellen:
```
Type:     HSQL Database Engine In-Memory
Driver:   org.hsqldb.jdbc.JDBCDriver
URL:      jdbc:hsqldb:file:./data/onlineshop
User:     SA
Password:
```