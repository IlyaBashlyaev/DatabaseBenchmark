## PostgreSQL (asynchrone Schreibweise)

### MacBook Pro M1
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 2827,5   | 89,3        | 2730     | 3052     |
| INSERT_BATCH   | 269,9    | 13,3        | 255      | 293      |
| SELECT_JOIN    | 29,9     | 2,0         | 28       | 34       |
| SELECT_DENORM  | 16,5     | 1,3         | 15       | 19       |

### Lenovo ThinkPad T422
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 3443,1   | 268,5       | 2778     | 3745     |
| INSERT_BATCH   | 976,9    | 181,6       | 692      | 1258     |
| SELECT_JOIN    | 135,1    | 8,2         | 123      | 149      |
| SELECT_DENORM  | 74,3     | 10,0        | 59       | 93       |

### MacBook Pro A2141
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 8764,4   | 644,5       | 7663     | 9962     |
| INSERT_BATCH   | 1982,9   | 125,5       | 1799     | 2239     |
| SELECT_JOIN    | 153,5    | 13,0        | 145      | 187      |
| SELECT_DENORM  | 89,1     | 7,3         | 74       | 95       |