classDiagram
    direction TB

    class DatabaseBenchmark {
        +main(String[] args)$
        -runBenchmark(BenchmarkMode mode, CsvLogger log)$
    }

    class BenchmarkMode {
        <<enumeration>>
        HSQLDB_OPTIMAL
        HSQLDB_EQUALIZED
        POSTGRESQL_OPTIMAL
        POSTGRESQL_EQUALIZED
    }

    class Db {
        +String driver
        +String url
        +getCon() Connection
        +close()
    }

    class InsertBenchmark {
        +runSingle(Db db, int customers, int products, CsvLogger log)$ long
        +runBatch(Db db, int customers, int products, int batchSize, CsvLogger log)$ long
    }

    class SelectBenchmark {
        +runAll(Db db, CsvLogger log)$ long
    }

    class DenormBenchmark {
        +populate(Db db)$
        +runAll(Db db, CsvLogger log)$ long
    }

    DatabaseBenchmark *-- BenchmarkMode

    DatabaseBenchmark ..> Db : creates
    DatabaseBenchmark ..> InsertBenchmark : uses
    DatabaseBenchmark ..> SelectBenchmark : uses
    DatabaseBenchmark ..> DenormBenchmark : uses

    InsertBenchmark ..> Db : uses
    SelectBenchmark ..> Db : uses
    DenormBenchmark ..> Db : uses