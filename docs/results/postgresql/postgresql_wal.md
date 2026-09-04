## PostgreSQL (WAL-Schreibweise)

### MacBook Pro M1
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 6088,8   | 103,4       | 5849     | 6206     |
| INSERT_BATCH   | 297,7    | 12,0        | 285      | 320      |
| SELECT_JOIN    | 28,7     | 3,5         | 25       | 38       |
| SELECT_DENORM  | 16,8     | 1,0         | 16       | 19       |

### Lenovo ThinkPad T422
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 9363,0   | 396,6       | 8253     | 9744     |
| INSERT_BATCH   | 885,3    | 116,2       | 695      | 1039     |
| SELECT_JOIN    | 95,9     | 14,9        | 66       | 112      |
| SELECT_DENORM  | 65,1     | 9,9         | 47       | 81       |

### MacBook Pro A2141
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 18731,4  | 1291,9      | 16900    | 21519    |
| INSERT_BATCH   | 2084,2   | 191,4       | 1875     | 2528     |
| SELECT_JOIN    | 137,5    | 20,9        | 112      | 184      |
| SELECT_DENORM  | 84,8     | 10,4        | 70       | 99       |