## HSQLDB (verzögerte Schreibweise)

### MacBook Pro M1
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 140,9    | 52,6        | 118      | 297      |
| INSERT_BATCH   | 119,3    | 29,3        | 105      | 206      |
| SELECT_JOIN    | 73,5     | 8,8         | 66       | 97       |
| SELECT_DENORM  | 12,6     | 0,9         | 12       | 15       |

### Lenovo ThinkPad T422
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 202,3    | 39,7        | 180      | 319      |
| INSERT_BATCH   | 185,5    | 24,4        | 172      | 257      |
| SELECT_JOIN    | 100,1    | 11,8        | 90       | 131      |
| SELECT_DENORM  | 11,1     | 0,8         | 10       | 12       |

### MacBook Pro A2141
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 520,6    | 196,3       | 409      | 1082     |
| INSERT_BATCH   | 432,2    | 74,6        | 378      | 637      |
| SELECT_JOIN    | 243,6    | 25,2        | 214      | 305      |
| SELECT_DENORM  | 40,1     | 6,2         | 30       | 53       |