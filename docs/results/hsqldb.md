# Ergebnisse für HSQLDB

Alle Zeitangaben in Millisekunden (ms). Jeder Wert basiert auf 10 Messwiederholungen mit 10.000 Datensätzen je Operation.

---

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

---

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