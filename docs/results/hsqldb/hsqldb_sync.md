## HSQLDB (synchrone Schriebweise)

### MacBook Pro M1

| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 1628,8   | 56,8        | 1562     | 1741     |
| INSERT_BATCH   | 119,6    | 18,5        | 107      | 173      |
| SELECT_JOIN    | 74,6     | 5,0         | 68       | 84       |
| SELECT_DENORM  | 13,2     | 2,1         | 12       | 19       |

### Lenovo ThinkPad T422
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 18444,0  | 4400,1      | 14774    | 24933    |
| INSERT_BATCH   | 330,2    | 39,0        | 294      | 426      |
| SELECT_JOIN    | 146,6    | 11,7        | 131      | 163      |
| SELECT_DENORM  | 20,3     | 4,2         | 12       | 28       |

### MacBook Pro A2141
| Operation      | Avg (ms) | Stddev (ms) | Min (ms) | Max (ms) |
|----------------|----------|-------------|----------|----------|
| INSERT_SINGLE  | 3960,1   | 347,9       | 3496     | 4557     |
| INSERT_BATCH   | 426,2    | 52,2        | 390      | 574      |
| SELECT_JOIN    | 223,5    | 13,8        | 209      | 251      |
| SELECT_DENORM  | 33,8     | 2,2         | 30       | 37       |