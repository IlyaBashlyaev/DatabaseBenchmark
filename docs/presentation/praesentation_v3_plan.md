# Präsentation v3 — план переработки

Источники: `log/benchmark_results.csv`, `docs/results/**`, `log/hardware/**`, `pom.xml`, исходники.
Исходное задание: `01_ITM-Blockwoche-DbBenchmark.pdf`.

Цифры замеров не меняются. Меняются: постановка вопроса, гипотеза, интерпретация, выводы, структура, оформление.

---

## 0. Принятые решения

| Решение | Статус |
|---|---|
| Программу не переписываем, повторных прогонов нет | принято |
| Режимов два: `synchron` / `asynchron` | принято |
| Графики: сгруппированные колонки, три колонки по железу на конфигурацию | принято |
| На графиках слева пара `synchron`, справа пара `asynchron` | принято |
| Слайд суммарной длительности убираем полностью | принято |
| DAO `Customer`/`Product` из кода не убираем | принято |
| Порядок изложения: СУБД → гипотеза → сценарий → что меряем → программа → объём → результаты → объяснение → выводы | принято |
| Код на слайдах: цикл замера, переключение режимов, генерация данных | принято |
| Светлая тема для скриншотов кода | принято |
| Тёмных слайдов и белого текста на тёмном нет — плохо читается через проектор | принято |
| На слайдах реальные скриншоты дерева проекта и кода из IntelliJ IDEA | принято |
| Единицы измерения вынесены на оси графиков, а не только в подпись | принято |

Главный принцип версии: **отрицательный результат — тоже результат.** Гипотеза не подтвердилась полностью, часть условий оказалась неидеальной. Это не прячем, а излагаем как ход работы. Бенчмарк проведён, данные получены, выводы сделаны из них, а не подогнаны под них.

---

## 1. Что было не так в v2

### 1.1 Сравнивались разные вещи

v2 сравнивает `HSQLDB (verzögert)` с `PostgreSQL (WAL)` и делает вывод «HSQLDB ist schneller». Это сравнение обещания с гарантией:

- HSQLDB в отложенном режиме подтверждает коммит **сразу**, запись на диск происходит позже.
- PostgreSQL в режиме WAL подтверждает коммит **после** того, как журнал физически записан.

Измерялась не скорость СУБД, а цена гарантии сохранности.

### 1.2 Презентация противоречила сама себе

Слайд 20: заголовок «HSQLDB ist schneller als PostgreSQL», строкой ниже «0,09 s — HSQLDB, 0,05 s — PostgreSQL». По чтению PostgreSQL быстрее на всех трёх машинах, и это уже было написано, но не осмыслено.

### 1.3 «Beste Auswahl» без указания требований

«MacBook Pro M1 + HSQLDB (verzögert)» — это самая быстрая измеренная конфигурация, а не лучший выбор. Рекомендация без указания требований к сохранности данных профессионально неверна.

### 1.4 Четыре названия режимов вместо двух

`verzögert`, `synchron`, `asynchron`, `WAL`. В коде ось названа `OPTIMAL` / `EQUALIZED`, и это ещё хуже: для HSQLDB «optimal» — асинхронный, для PostgreSQL «optimal» — синхронный.

### 1.5 Неверная причина скорости HSQLDB

v2 утверждает «Sehr schnell wegen des RAM-Zugriffs» и «Arbeitet auf dem Arbeitsspeicher-Niveau». `DataGenerator.createSchema()` создаёт `CACHED TABLE` — строки лежат в файле `./data/onlineshop.data`, в памяти только LRU-кэш. Это файловая база, встроенная в процесс, а не in-memory.

Формулировка не выдержит одного вопроса: «если дело в памяти, почему ваш же синхронный замер на той же базе медленнее в 11,6 раза?» Ответить будет нечем.

Правильная причина — та, которую вы сформулировали с самого начала: **HSQLDB подтверждает коммит, не дожидаясь диска, поэтому ожидание записи не попадает в окно измерения.**

### 1.6 Ошибка в объёме данных

Одна операция `INSERT_SINGLE` вставляет **40 000 строк**: по 10 000 в `CUSTOMER`, `PRODUCT`, `INVOICE`, `ITEM` (`INVOICES_PER_CUSTOMER = 1`, `ITEMS_PER_INVOICE = 1`). Подпись «10.000 Datensätze» относится к одной таблице, не к операции.

---

## 2. Новая сквозная линия

```
Zwei Systeme  →  Erwartung  →  Messung  →  Widerspruch  →  Ursache  →  Grenzen  →  Empfehlung
```

1. Две СУБД: встроенная файловая против серверной. Из этого выводится гипотеза.
2. Гипотеза: HSQLDB работает в памяти и потому быстрее во всём.
3. Замер: по записи до 11× быстрее, по чтению с JOIN — **медленнее** на всех трёх машинах, а на Windows в синхронном режиме вообще худший результат.
4. Противоречие: гипотеза подтверждается наполовину, значит причина названа неверно.
5. Настоящая причина: момент подтверждения коммита и накладные расходы на каждый оператор.
6. Границы: часть условий не была идеально симметричной. Называем сами.
7. Рекомендация по назначению, а не по абсолютной скорости.

---

## 3. Что говорят цифры

### 3.1 Стоимость одного коммита

`INSERT_SINGLE` — 40 000 коммитов (`setAutoCommit(true)`), `INSERT_BATCH` — 40 коммитов (`flush()` = `executeBatch` + `commit` на каждые 1000 строк, четыре таблицы). SQL одинаковый. Разница делится на 39 960.

| Konfiguration | M1 | ThinkPad | A2141 |
|---|---|---|---|
| HSQLDB asynchron | **0,5 µs** | **0,4 µs** | **2,2 µs** |
| HSQLDB synchron | 37,8 µs | **453,3 µs** | 88,4 µs |
| PostgreSQL asynchron | 64,0 µs | 61,7 µs | 169,7 µs |
| PostgreSQL synchron | 144,9 µs | 212,2 µs | 416,6 µs |

Важная оговорка: для HSQLDB это действительно цена коммита — она встроенная, других накладных расходов на вызов нет. Для PostgreSQL сюда же входит цена обращения к серверу, и разделить их этим замером нельзя. Поэтому на слайд идут не микросекунды, а «три рычага».

### 3.2 Три рычага — какой сильнее

Отправная точка — худшая конфигурация: PostgreSQL synchron, поштучная вставка. Во сколько раз ускоряет каждый отдельный приём:

| Что меняем | M1 | ThinkPad | A2141 |
|---|---|---|---|
| Пакетная вставка вместо поштучной | **20,5×** | **10,6×** | **9,0×** |
| Другая СУБД (HSQLDB, тот же режим) | 3,7× | 0,5× | 4,7× |
| Отказ от гарантии записи | 2,2× | 2,7× | 2,1× |

Батчинг выигрывает на всех трёх машинах. Смена СУБД на ThinkPad уходит в минус. Главный рычаг производительности — модель транзакций в приложении, а не выбор продукта.

### 3.3 Цена одного оператора

Обе БД в асинхронном режиме, время на одну строку, M1:

| | HSQLDB | PostgreSQL | Разрыв |
|---|---|---|---|
| `INSERT_SINGLE` — 40 000 операторов | 3,52 µs | 70,69 µs | 67,17 µs |
| `INSERT_BATCH` — 40 операторов | 2,98 µs | 6,75 µs | 3,76 µs |

Сокращение числа операторов с 40 000 до 40 схлопывает разрыв между СУБД с 67,17 до 3,76 µs на строку на M1. На ThinkPad с 81,0 до 19,8, на A2141 с 206,1 до 38,8. Направление одинаковое на всех трёх машинах, величина эффекта разная.

Разрыв создают накладные расходы **на каждый оператор**, а не работа с данными. Что в них весит больше — круговой обмен с сервером или обслуживание транзакции — этот замер не разделяет: между `SINGLE` и `BATCH` меняются обе величины сразу (обращений 40 000 → ~80, транзакций 40 000 → 40). Для разделения нужен был бы прогон HSQLDB в серверном режиме: тот же движок, добавленный транспорт.

Кратность («схлопнулось в 18 раз») в основной текст не выносим: на M1 разрыв в пакетном режиме 3,76 µs при стандартном отклонении соответствующих замеров около 0,8 µs. Разброс по машинам от 4× до 18× это подтверждает — устойчиво только направление.

Формулировать надо не «задержка сети», а **стоимость одного оператора**. У встроенной HSQLDB это вызов метода внутри той же JVM, у PostgreSQL — круговой обмен по loopback, разбор протокола и обслуживание транзакции на стороне сервера.

### 3.4 Чтение: гипотеза опровергнута

Лучшее значение каждой СУБД, мс:

| Operation | | M1 | ThinkPad | A2141 |
|---|---|---|---|---|
| `SELECT_JOIN` | HSQLDB | 73,5 | 100,1 | 223,5 |
| | PostgreSQL | **28,7** | **95,9** | **137,5** |
| | | PostgreSQL 2,6× | 1,0× | 1,6× |
| `SELECT_DENORM` | HSQLDB | **12,6** | **11,1** | **33,8** |
| | PostgreSQL | 16,5 | 65,1 | 84,8 |
| | | HSQLDB 1,3× | 5,9× | 2,5× |

Разделение воспроизводится на всех трёх машинах:

- Сложный запрос с JOIN по четырём таблицам → выигрывает PostgreSQL. Работает оптимизатор запросов, время обращения к серверу уже не играет роли.
- Простой запрос по одной таблице → выигрывает HSQLDB. Работы почти нет, доминирует путь до сервера.

Отдельно: отложенная запись на чтение не влияет вообще, режимы `synchron` и `asynchron` дают одинаковые значения. Это видно на графике и снимает вопрос само собой.

### 3.5 Воспроизводимость

Относительное стандартное отклонение, `INSERT_SINGLE`:

| Konfiguration | M1 | ThinkPad | A2141 |
|---|---|---|---|
| HSQLDB asynchron | 37,3 % | 19,6 % | 37,7 % |
| HSQLDB synchron | 3,5 % | **23,9 %** | 8,8 % |
| PostgreSQL asynchron | 3,2 % | 7,8 % | 7,4 % |
| PostgreSQL synchron | **1,7 %** | **4,2 %** | **6,9 %** |

Самая быстрая конфигурация одновременно самая нестабильная: интервал сброса 500 мс попадает в измерение случайным образом. PostgreSQL synchron — самая предсказуемая.

### 3.6 Аномалия ThinkPad

HSQLDB synchron, `INSERT_SINGLE`: M1 1,63 с · A2141 3,96 с · **ThinkPad 18,44 с**. Единственная конфигурация, где HSQLDB — худшая из четырёх.

Три доказательства причины, все из собственных данных:

1. **Железо ни при чём.** На том же ThinkPad PostgreSQL synchron даёт 9,36 с — вдвое быстрее, чем A2141 (18,73 с).
2. **Платим за коммит, не за SQL.** В пакетном режиме на ThinkPad разрыв между sync и async всего 1,8× (330,2 мс против 185,5 мс).
3. **Внешнее вмешательство.** σ = 23,9 % против 3,5 % на M1. Стабильная стоимость так не выглядит.

Вероятная причина: на Windows `FlushFileBuffers` продавливает кэш накопителя, плюс Defender проверяет каждую запись в файл. На macOS `fsync()` доводит данные только до контроллера диска — для полного сброса нужен `F_FULLFSYNC`, которого Java не вызывает. То есть HSQLDB `synchron` на macOS даёт **более слабую гарантию**, чем на Windows, под тем же названием режима.

Формулировать как `wahrscheinliche Ursache` с тремя доказательствами.

### 3.7 Скорость против сохранности — цена каждого параметра

Данные для нового слайда 16. Пять параметров, которыми можно влиять на скорость, с выигрышем и с тем, чем за него платим.

**Выигрыш от каждого параметра**

| Параметр | Выигрыш при поштучной вставке | Выигрыш при пакетной |
|---|---|---|
| Пакетная вставка вместо поштучной | 9,0× – 20,5× | — |
| HSQLDB: `WRITE DELAY TRUE` вместо `FALSE` | 7,6× – **91×** | 1,0× – 1,8× |
| PostgreSQL: `synchronous_commit = off` | 2,1× – 2,7× | 0,9× – 1,1× |
| PostgreSQL: таблицы `UNLOGGED` | не измерено отдельно | не измерено отдельно |
| Денормализация при чтении | HSQLDB 5,7× – 7,2× · PostgreSQL 1,5× – 1,8× | |

91× у HSQLDB — это ThinkPad, то есть аномалия из 3.6. Без неё диапазон 7,6× – 11,6×.

**Денормализация: в каких режимах**

Диапазон взят по **обоим режимам и трём машинам** — шесть значений на СУБД. Режим записи на чтение не влияет, поэтому агрегировать законно, и данные это подтверждают:

| СУБД | Режим | M1 | ThinkPad | A2141 |
|---|---|---|---|---|
| HSQLDB | synchron | 74,6 / 13,2 = 5,7× | 146,6 / 20,3 = 7,2× | 223,5 / 33,8 = 6,6× |
| HSQLDB | asynchron | 73,5 / 12,6 = 5,8× | 100,1 / 11,1 = **9,0×** | 243,6 / 40,1 = 6,1× |
| PostgreSQL | synchron | 28,7 / 16,8 = 1,7× | 95,9 / 65,1 = 1,5× | 137,5 / 84,8 = 1,6× |
| PostgreSQL | asynchron | 29,9 / 16,5 = 1,8× | 135,1 / 74,3 = 1,8× | 153,5 / 89,1 = 1,7× |

Внутри каждой СУБД режимы дают почти одинаковые числа — разница между строками меньше, чем между машинами.

Две оговорки:

1. **Верхняя граница 9,0× шаткая.** Она держится на единственном значении: `SELECT_DENORM` на ThinkPad в асинхронном режиме — 11,1 мс, при σ = 0,8 мс. Это единственный замер во всей работе, где ThinkPad быстрее M1 (12,6 мс). Скорее всего шум. Без него диапазон HSQLDB 5,7× – 7,2×. **На слайд выносить 5,7× – 7,2×**, 9,0× не использовать.
2. **Запросы сопоставимы, объём данных — нет.** `FLAT_SALES` это 10 000 строк, JOIN идёт по четырём таблицам с 40 000 строк суммарно. Денормализованное чтение по построению трогает вчетверо меньше данных. Это свойство приёма, а не дефект замера, но сказать надо.

Мелочь для полноты: Q1 в нормализованном варианте использует `LEFT JOIN` (все клиенты), а `FLAT_SALES` построена через `INNER JOIN`. При `INVOICES_PER_CUSTOMER = 1` у каждого клиента ровно один счёт, поэтому обе формы возвращают одни и те же 10 000 строк.

**Чем платим**

| Параметр | Что теряем при аварии | В строках, при измеренной скорости |
|---|---|---|
| Пакетная вставка | **ничего подтверждённого** — откатывается до 999 неподтверждённых строк | 0 |
| HSQLDB `WRITE DELAY TRUE` | до 500 мс подтверждённых транзакций | ≈ 140 000 |
| PostgreSQL `synchronous_commit = off` | последние сотни мс подтверждённых транзакций (окно до 3 × `wal_writer_delay`) | ≈ 8 500 |
| PostgreSQL `UNLOGGED` | **вся таблица целиком** | все 40 000 |
| Денормализация | данных не теряем, но теряем целостность: избыточность, нет внешних ключей, аномалии при обновлении. Плюс построение `FLAT_SALES` **в замер не входит** — `populate()` вызывается до таймера | — |

**Два вывода, которые делают слайд**

1. **Самый сильный рычаг — единственный бесплатный.** Пакетная вставка даёт 9×–20,5× и не стоит ни одной подтверждённой строки.
2. **Если батчить, асинхронный режим почти ничего не даёт.** В пакетном режиме отказ от гарантии стоит от −9 % до +10 % на пяти конфигурациях из шести. Исключение одно — HSQLDB на ThinkPad, 78 %, и это та же аномалия. То есть риск можно просто не брать.

Проверка по замерам, `INSERT_BATCH`, synchron против asynchron:

| | M1 | ThinkPad | A2141 |
|---|---|---|---|
| HSQLDB | 119,6 / 119,3 = 1,00× | 330,2 / 185,5 = 1,78× | 426,2 / 432,2 = 0,99× |
| PostgreSQL | 297,7 / 269,9 = 1,10× | 885,3 / 976,9 = 0,91× | 2084,2 / 1982,9 = 1,05× |

Значения меньше 1,0 означают, что синхронный режим оказался быстрее — разница в пределах разброса.

Оговорка для честности: `BATCH_SIZE = 1000` мы не варьировали. Больший пакет означал бы ещё меньше коммитов, но и больше строк под откатом, и больше памяти. Это параметр, который в работе не исследован.

---

## 4. Границы сравнения — что называем сами

Это новый обязательный слайд. Задание требует `Überprüfung auf Kausalität und Messfehler` и `Gibt es widersprüchliche Ergebnisse, wenn ja, wie kann man die Untersuchung verbessern?`

### Что было одинаково

Код, схема, типы полей, объём (40 000 строк за прогон), число повторов, модель транзакций, окно измерения, три машины. Это большая часть условий, и она соблюдена.

### Что не было одинаково

| | Насколько серьёзно |
|---|---|
| У PostgreSQL в асинхронном режиме таблицы `UNLOGGED` — журнал не пишется вообще, при аварии таблица очищается целиком. У HSQLDB журнал пишется, теряется до ~500 мс | Реальная асимметрия. Гарантия у PostgreSQL слабее, чем у HSQLDB |
| Между прогонами `DELETE`, а не `TRUNCATE` | Мелочь. Разброс min/max у PostgreSQL 5849…6206 мс, накопления не видно |
| HSQLDB встроенная, PostgreSQL серверная | Не дефект, а природа продуктов. Уравнять нельзя, не превратив один из них в то, чем он не является |

### Как это подать

Асимметрия работает **на вас**, а не против. Раз у PostgreSQL сохранность отключена даже сильнее, отставание в 11× нельзя списать на «он всё равно что-то дописывал на диск». Не дописывал. Значит остаток разницы — это цена обращения к серверу, и это доказывается из ваших же цифр.

```
Unsere Messung vergleicht zwei Systeme, die sich nicht in allen Punkten
gleichsetzen lassen. Das haben wir erst bei der Auswertung erkannt.
Wir legen es offen, weil es die Ergebnisse nicht entwertet, sondern erklärt.
```

Это и есть «отрицательный результат — тоже результат»: не оправдание, а часть методической работы.

---

## 5. Структура: 23 слайда

| # | Folie | Что на нём |
|---|---|---|
| 1 | Deckblatt | Тема как вопрос |
| 2 | Agenda | Три части |
| 3 | Ausgangssituation und Fragestellung | Задача и вопрос |
| 4 | **Die beiden Datenbanksysteme** | Ключевые отличия — основа гипотезы |
| 5 | **Erwartung** | Гипотеза |
| 6 | Szenario: Onlineshop | ER-диаграмма |
| 7 | Was gemessen wird | 4 операции + 2 оптимизации |
| 8 | **Die zwei Schreibmodi** | Матрица 2×2 |
| 9 | Das Benchmark-Programm | Дерево + диаграмма классов |
| 10 | Code: Messung und Modus | Два фрагмента |
| 11 | Code: Testdaten | `DataGenerator` |
| 12 | Testrechner, Umfang und Reproduzierbarkeit | Железо, объём, 10 повторов |
| 13 | **Ergebnisse: Schreibzugriffe** | График |
| 14 | **Ergebnisse: Lesezugriffe** | График |
| 15 | **Drei Hebel** | Объяснение |
| 16 | **Geschwindigkeit gegen Sicherheit** | Цена каждого параметра |
| 17 | Die Windows-Anomalie | Разбор аномалии |
| 18 | **Hypothese und Ergebnis** | Soll-Ist |
| 19 | **Grenzen des Vergleichs** | Честная самокритика |
| 20 | Erkenntnisse und Empfehlung | Выводы |
| 21 | Ausblick | Что дальше |
| 22 | Dank und Fragen | |
| 23 | Quellenverzeichnis | |

---

## 6. Слайд за слайдом

Правило для всех: не больше 8 коротких блоков, никаких целых предложений на слайде кроме одной итоговой строки.

---

### Folie 1 — Deckblatt

```
DB-Benchmark — HSQLDB und PostgreSQL
Was kostet die Sicherheit einer Transaktion?

Illia Kondrat · Pavel Polukhin · Illia Bashliaiev
Kurs 26ITO7 · GIT/PPS · Blockwochenprojekt
Robert-Bosch-Berufskolleg Dortmund · 23.09.2026
```

Подзаголовок-вопрос сразу задаёт настоящую тему и снимает половину вопросов.

---

### Folie 2 — Agenda

```
1  Fragestellung und Erwartung
2  Testszenario und Messverfahren
3  Ergebnisse
4  Auswertung und Erkenntnisse
```

---

### Folie 3 — Ausgangssituation und Fragestellung

```
Ausgangssituation
Ein Onlineshop speichert Bestelldaten.

Zur Auswahl stehen zwei grundverschiedene Wege:

·  eine eingebettete Datenbank, die in der Anwendung selbst läuft
·  ein Datenbankserver, der als eigener Dienst arbeitet

Hauptfrage
Welches System eignet sich wofür — und was kostet die Zusage,
dass eine Transaktion wirklich gespeichert ist?

Teilfragen
Wie stark wirkt das Transaktionsmodell: Einzel-Commit gegen Batch?
Wie wirkt Normalisierung gegen Denormalisierung beim Lesen?
Wie stark hängen die Ergebnisse von Hardware und Betriebssystem ab?
```

---

### Folie 4 — Die beiden Datenbanksysteme

Правки к таблице из v2 — здесь меняются именно причины, а не слова:

| Было | Стало |
|---|---|
| `Arbeitet auf dem Arbeitsspeicher-Niveau` | `Läuft eingebettet in der Anwendung — kein Serverdienst, kein Netzwerkweg` |
| `Sehr schnell wegen des RAM-Zugriffs` | `Sehr schnell, weil ein Commit nicht auf die Festplatte wartet` |
| `Langsamer als HSQLDB wegen der Latenz in der TCP-Verbindung` | `Jeder Zugriff läuft über einen Serverdienst — bei kurzen Abfragen fällt dieser Weg ins Gewicht` |

Добавить в `Nachteile` HSQLDB:

```
Im Standardbetrieb wird ein Commit bestätigt, bevor die Daten auf der Festplatte liegen
```

Итоговый вид:

```
                     HSQLDB                        PostgreSQL

Betrieb              eingebettet in der JVM        eigener Serverdienst
                     Daten in ./data als Datei     Daten auf dem Server

Vorteile             kein Serverweg, ein JAR       skalierbar und stabil im Produktivbetrieb
                     Commit wartet nicht auf        starke SQL-Features,
                     die Festplatte                 hohe Standardkonformität

Nachteile            Commit wird bestätigt,        jeder Zugriff über einen Serverdienst
                     bevor geschrieben wurde        Installation und Administration aufwendiger
                     eingeschränkte SQL-Features
```

---

### Folie 5 — Erwartung

```
Unsere Erwartung vor der Messung

Hypothese
„HSQLDB arbeitet im Arbeitsspeicher und ist deshalb in allen Operationen
schneller als PostgreSQL."

Begründung
kein Serverweg, kein Verbindungsaufbau
schlanke Architektur: eine JAR-Datei statt eines Serverdienstes
Daten liegen nahe am Prozess

Erwartetes Ergebnis
HSQLDB gewinnt beim Schreiben und beim Lesen, auf jeder Hardware.
```

Тезис про память оставляем **здесь** — это то, во что вы верили до измерений. Опровергается он на слайде 17. Тогда правка ничего не отнимает: вместо «мы ошиблись в описании продукта» получается «мы проверили собственное предположение и нашли настоящую причину».

На слайде вместо спойлера стоит интрига: `Ob das stimmt, entscheidet die Messung — die Antwort steht in den Erkenntnissen.` Результат не называем, иначе вся вторая половина доклада теряет напряжение.

---

### Folie 6 — Szenario: Onlineshop

Готовый файл: `docs/er_diagram/er_diagram.svg`

Слева четыре нормализованные таблицы с настоящими связями, справа `FLAT_SALES`. У каждого поля плоской таблицы цветная полоска в цвете таблицы-источника — видно, что схлопнуты все четыре, а не две. Между ними пунктирная стрелка `abgeleitet`, а не «один ко многим»: внешних ключей у `FLAT_SALES` нет.

```
Vier normalisierte Tabellen: CUSTOMER, PRODUCT, INVOICE, ITEM
Eine abgeleitete Tabelle: FLAT_SALES
Identisches Schema in beiden Datenbanksystemen
```

---

### Folie 7 — Was gemessen wird

```
Zwei Grundoperationen

Schreiben   Tabellen mit Testdaten füllen
Lesen       drei Abfragen über die gefüllten Tabellen

Zwei Optimierungen, die wir dagegenhalten

Batch          1.000 Zeilen je Commit statt einer
Denormalisiert alle Daten in einer Tabelle statt JOIN über vier

Daraus vier Messoperationen

INSERT_SINGLE   jede Zeile einzeln, Autocommit
INSERT_BATCH    Pakete von 1.000 Zeilen
SELECT_JOIN     drei Abfragen über bis zu vier verknüpfte Tabellen
SELECT_DENORM   dieselben drei Abfragen auf FLAT_SALES
```

---

### Folie 8 — Die zwei Schreibmodi

Матрица вместо списка из четырёх пунктов. Эта правка чинит логику всей презентации.

```
                  synchron                      asynchron
                  Commit wartet auf die         Commit wird bestätigt,
                  Festplatte                    bevor geschrieben wird

HSQLDB            SET FILES WRITE DELAY FALSE   Schreiben alle 500 ms

PostgreSQL        WAL, Standardeinstellung      synchronous_commit = off
                                                Tabellen UNLOGGED

Vergleichbar sind nur Konfigurationen derselben Spalte.

Commit = der Zeitpunkt, an dem eine Datenbank eine Transaktion
als endgültig abgeschlossen markiert.
```

Устная реплика: `Die linke Spalte gibt eine Zusage, die rechte Spalte gibt ein Versprechen.`

Строку `Tabellen UNLOGGED` оставляем намеренно — она подготавливает слайд 18.

---

### Folie 9 — Das Benchmark-Programm

Скриншот дерева проекта из IntelliJ IDEA слева, `docs/class_diagram/simple/class_diagram.svg` справа. Диаграмма перерисована: три пакета рамками, 11 классов без атрибутов и методов, у каждого одна подпись-функция.

`Customer` и `Product` подписаны `ORM-Übung, nicht im Messpfad` — вопрос «а где они используются?» снимается до того, как его зададут.

```
Drei Packages
benchmark   Messlogik und Testdatengenerierung
dao         JDBC-Verbindung
util        Statistik, CSV-Protokoll, Systemdaten

Gemessen wird ausschließlich die Zeit zwischen Absenden und Rückkehr
des SQL-Aufrufs — ohne Objekt-Mapping, ohne Datengenerierung.
```

Последняя строка отвечает на вопрос «вы уверены, что мерили базу, а не Java?» и заодно объясняет, почему DAO для `INVOICE` и `ITEM` не нужны.

---

### Folie 10 — Code: Messung und Modus

Два фрагмента, светлая тема IDE.

**Фрагмент 1 — окно измерения** (`InsertBenchmark.flush`, строки 246–251):

```java
private static long flush(Connection con, PreparedStatement ps) throws SQLException {
    long s = System.nanoTime();
    ps.executeBatch();
    con.commit();
    return System.nanoTime() - s;
}
```

**Фрагмент 2 — переключение режимов** (`DatabaseBenchmark`, строки 77–83, 100–104, 116):

```java
if (mode.isHsqlSync) { db.url += ";hsqldb.write_delay=false"; }
if (mode.isPgAsyncUnlogged) { db.url += "?options=-c%20synchronous_commit=off"; }
...
s.execute(mode.isHsqlSync ? "SET FILES WRITE DELAY FALSE" : "SET FILES WRITE DELAY TRUE");
```

```
Der Zeitnehmer umschließt nur den SQL-Aufruf.
Die vier Konfigurationen unterscheiden sich in genau zwei Einstellungen.
```

---

### Folie 11 — Code: Testdaten

Скриншот `DataGenerator` — массивы имён и метод генерации.

```java
private static final Random RNG = new Random(42);

public static String productName() {
    return PRODUCT_ADJ[RNG.nextInt(PRODUCT_ADJ.length)]
         + " " + PRODUCT_NOUNS[RNG.nextInt(PRODUCT_NOUNS.length)];
}
```

```
Namen, Adressen und Produkte werden aus festen Wortlisten zufällig kombiniert.
Preise, Kosten und Mengen werden zufällig berechnet.

Fester Startwert 42: jeder Lauf auf jedem Rechner erzeugt dieselben Daten.
Unterschiede in den Messwerten können nicht von den Daten kommen.
```

Последняя строка важна: фиксированный seed — это аргумент за воспроизводимость, и его стоит назвать явно.

---

### Folie 12 — Testrechner, Umfang und Reproduzierbarkeit

```
Drei Testrechner
MacBook Pro (2021)          Apple M1 Pro          32 GB   macOS 26.5
Lenovo ThinkPad P16 Gen 1   Intel Core i7-12800HX 32 GB   Windows 11
MacBook Pro A2141 (2019)    Intel Core i7-9750H   16 GB   macOS 15.5

Umfang je Messung
10.000 Zeilen in jede der vier Tabellen = 40.000 Zeilen
INSERT_SINGLE   40.000 Commits
INSERT_BATCH    40 Commits
SELECT          drei Abfragen zu je 10.000 Zeilen = 30.000 Zeilen

Gesamtumfang
1.400.000 Zeilen je Konfiguration · 5.600.000 Zeilen insgesamt

Wiederholungen
10 Durchläufe je Operation · 4 Konfigurationen · 3 Rechner
10 Durchläufe unterdrücken zufällige Messabweichungen.
3 Rechner zeigen, ob ein Ergebnis an der Datenbank oder an der Hardware liegt.

Relative Standardabweichung bei INSERT_SINGLE
PostgreSQL synchron    1,7 % – 6,9 %    sehr gut reproduzierbar
HSQLDB asynchron      19,6 % – 37,7 %   stark schwankend
```

Последняя пара строк — сильный ход: самая быстрая конфигурация оказывается наименее предсказуемой, и это готовит слайд 20.

Немецкий термин: **Reproduzierbarkeit**. Повторы убирают `zufällige Messabweichung`, три машины отсекают систематическую ошибку конкретной машины.

Числа проверены по `log/benchmark_results.csv`: столбец `Rows` равен 10 000 для всех четырнадцати подопераций. Отсюда `SELECT_JOIN` = 3 × 10 000 × 10 = 300 000 строк, `INSERT_SINGLE` = 4 × 10 000 × 10 = 400 000. Цифры v2 подтвердились.

Блок с железом раньше стоял отдельным слайдом (v2, слайд 9). Здесь он на месте: графики ссылаются на три машины, и читатель должен знать их до слайда 13.

---

### Folie 13 — Ergebnisse: Schreibzugriffe

Готовый файл: `docs/results/charts/schreiben.svg`

Две панели: `Einzeln` (40 000 коммитов) и `Gebündelt` (40 коммитов). В каждой слева пара `synchron`, справа пара `asynchron`, в каждой конфигурации три колонки по железу. Логарифмическая ось — разброс 133×, на линейной самая быстрая колонка не видна. Аномалия ThinkPad выделена красным.

Итоговые строки на панелях:

```
Faktor 133 zwischen schnellster und langsamster Messung
Bei 40 Commits kostet die Garantie fast nichts
```

---

### Folie 14 — Ergebnisse: Lesezugriffe

Готовый файл: `docs/results/charts/lesen.svg`

Панели `Mit JOIN` и `Denormalisiert`, та же группировка.

```
PostgreSQL gewinnt in beiden Modi
HSQLDB gewinnt in beiden Modi

Der Schreibmodus hat auf das Lesen keinen Einfluss —
die linke und die rechte Gruppe sind praktisch identisch.

Komplexe Abfrage: der Abfrageoptimierer entscheidet, PostgreSQL ist stärker.
Einfache Abfrage: kaum Arbeit zu verteilen, der Serverweg dominiert.

Die drei Abfragen sind in beiden Varianten gleich aufgebaut —
gleiche Spalten, gleiche Gruppierung, gleiche Sortierung.
Nur das FROM unterscheidet sich.
```

Последний абзац нужен: без него первый же вопрос будет «а вы уверены, что сравнивали схемы, а не разные запросы?» Ответ проверен по коду — `SELECT_JOIN` и `SELECT_DENORM` совпадают во всём, кроме `FROM`.

Это прямо опровергает формулировку v2 «langsamer wegen der Latenz» как общее правило: путь до сервера решает только тогда, когда сам запрос дешёвый.

---

### Folie 15 — Drei Hebel

График: три группы по железу, в каждой три колонки — во сколько раз ускоряет каждый приём. Отправная точка — PostgreSQL synchron, поштучная вставка.

```
Welcher Eingriff wirkt am stärksten?

                                       M1      ThinkPad   A2141
Batch statt Einzel-Commit             20,5x     10,6x      9,0x
Anderes Datenbanksystem                3,7x      0,5x      4,7x
Garantie aufgeben                      2,2x      2,7x      2,1x

Der stärkste Hebel liegt in der Anwendung, nicht im Datenbankprodukt.

Gegenprobe: dieselbe Zeile, beide Systeme asynchron, MacBook Pro M1
40.000 Anweisungen   HSQLDB 3,52 µs   PostgreSQL 70,69 µs   Abstand 67,17 µs
40 Anweisungen       HSQLDB 2,98 µs   PostgreSQL  6,75 µs   Abstand  3,76 µs

Den Abstand erzeugt der Aufwand je Anweisung, nicht die Arbeit an den Daten.
```

Что **нельзя** утверждать на этом слайде: что мы «убрали обращения к серверу». Их число сократилось с 40 000 до примерно 80 (`addBatch()` сетевого обмена не делает, копит параметры в драйвере). И одновременно число транзакций упало с 40 000 до 40. Две величины меняются вместе, и этот замер их не разделяет.

Кратность «в 18 раз» в текст не выносим: разрыв 3,8 µs при σ около 0,8 µs. Разброс по машинам 4×…18× подтверждает, что устойчиво только направление.

Микросекунды на коммит на слайд тоже не выносим: у HSQLDB это чистая цена коммита, у PostgreSQL в неё примешана цена обращения к серверу. В запасе держим для Fachgespräch.

---

### Folie 16 — Geschwindigkeit gegen Sicherheit

Новый слайд. Собирает в одно место всё, чем можно влиять на скорость, и ставит рядом цену каждого приёма. Данные — раздел 3.7.

```
Fünf Stellschrauben — was sie bringen und was sie kosten

Stellschraube                         Gewinn          Verlust bei einem Absturz

Batch statt Einzel-Commit             9x – 20,5x      nichts Bestätigtes
                                                      bis 999 offene Zeilen zurückgerollt

HSQLDB  WRITE DELAY TRUE              7,6x – 11,6x    bis 500 ms bestätigter Transaktionen
                                                      rund 140.000 Zeilen

PostgreSQL  synchronous_commit=off    2,1x – 2,7x     die letzten paar hundert ms
                                                      rund 8.500 Zeilen

PostgreSQL  UNLOGGED-Tabellen         nicht getrennt  die ganze Tabelle
                                      gemessen        alle 40.000 Zeilen

Denormalisiert lesen                  HSQLDB 5,7x–7,2x kein Datenverlust, aber Redundanz,
(beide Modi, drei Rechner)            PG 1,5x–1,8x     keine Fremdschlüssel, Anomalien
                                                       Aufbau von FLAT_SALES nicht mitgemessen
```

Две итоговые строки — они и есть смысл слайда:

```
Der stärkste Hebel ist der einzige, der nichts kostet.

Wer bündelt, braucht das Risiko gar nicht:
im Batch-Betrieb kostet die volle Garantie zwischen -9 % und +10 %.
```

Проверка второго утверждения для вопросов — `INSERT_BATCH`, synchron против asynchron:

| | M1 | ThinkPad | A2141 |
|---|---|---|---|
| HSQLDB | 1,00× | 1,78× | 0,99× |
| PostgreSQL | 1,10× | 0,91× | 1,05× |

Пять значений из шести в пределах ±10 %. Шестое — ThinkPad с HSQLDB, та же аномалия, о которой следующий слайд.

Это лучший переход к слайду 17: единственное исключение из правила и есть тема следующего слайда.

Диапазон для HSQLDB на слайде указан без ThinkPad (7,6×–11,6× вместо 7,6×–91×) — аномальное значение разбирается отдельно, смешивать его с нормальным диапазоном нельзя. Сказать об этом одной строкой в докладе.

Оговорка, если спросят: `BATCH_SIZE = 1000` не варьировался. Больший пакет — меньше коммитов, но больше строк под откатом и больше памяти. Этот параметр в работе не исследован.

---

### Folie 17 — Die Windows-Anomalie

```
HSQLDB synchron, Einzel-Commit
M1 1,63 s · A2141 3,96 s · ThinkPad 18,44 s

Die einzige Konfiguration, in der HSQLDB das schlechteste Ergebnis liefert.

Drei Belege aus unseren eigenen Daten
Die Hardware ist in Ordnung: PostgreSQL synchron läuft auf dem ThinkPad
  mit 9,36 s doppelt so schnell wie auf dem A2141 mit 18,73 s.
Gezahlt wird je Commit: im Batch-Betrieb beträgt der Abstand nur 1,8x.
Äußere Störung: Standardabweichung 23,9 % gegen 3,5 % auf dem M1.

Wahrscheinliche Ursache
Unter Windows leert FlushFileBuffers den Cache des Laufwerks wirklich,
zusätzlich prüft der Virenscanner jeden Schreibvorgang.
Unter macOS schiebt fsync() die Daten nur bis zum Controller.

Folge: HSQLDB synchron gibt unter macOS eine schwächere Zusage
als unter Windows — unter demselben Namen.
```

Это ровно то, что задание называет `Überprüfung auf Kausalität und Messfehler`.

---

### Folie 18 — Hypothese und Ergebnis

```
Soll-Ist-Vergleich

Erwartet: HSQLDB ist in allen Operationen schneller.

bestätigt    Schreiben ohne Garantie            11x schneller
bestätigt    einfaches Lesen, denormalisiert    1,3x bis 5,9x schneller
teilweise     Schreiben mit Garantie             3,7x–4,7x schneller auf macOS,
                                                 1,8x langsamer unter Windows
widerlegt     Lesen mit JOIN                     PostgreSQL 1,0x–2,6x schneller

Nicht die Speichertechnik war der Unterschied, sondern der Zeitpunkt
der Commit-Bestätigung. Beide Systeme schreiben auf die Festplatte —
HSQLDB wartet im Standardbetrieb nur nicht darauf.
```

Последний абзац — самое ценное на всей презентации. Опровергнутая гипотеза с найденной настоящей причиной сильнее, чем подтверждённая гипотеза без причины.

---

### Folie 19 — Grenzen des Vergleichs

```
Was identisch war
Code, Schema, Datenmenge, Wiederholungen, Transaktionsmodell,
Messfenster, dieselben drei Rechner, fester Zufalls-Startwert.

Was nicht identisch war
PostgreSQL asynchron nutzt UNLOGGED-Tabellen: kein Journal, bei einem
  Absturz geht die ganze Tabelle verloren. HSQLDB asynchron führt ein
  Journal und verliert höchstens 500 ms.
Zwischen den Durchläufen DELETE statt TRUNCATE.
HSQLDB ist eingebettet, PostgreSQL ist ein Serverdienst — das lässt
  sich nicht gleichsetzen, ohne eines der Produkte zu verfälschen.

Warum das die Ergebnisse nicht entwertet
PostgreSQL hatte in der asynchronen Messung die schwächere Zusage.
Der Rückstand kann also nicht daran liegen, dass es trotzdem geschrieben hätte.
Was bleibt, ist der Preis des Serverzugriffs — und genau den zeigt Folie 15.

Wir haben diese Unterschiede erst bei der Auswertung erkannt
und legen sie offen.
```

Это тот слайд, который отличает работу от отчёта. Задание прямо требует назвать противоречия и сказать, как исследование можно улучшить.

---

### Folie 20 — Erkenntnisse und Empfehlung

```
Erkenntnisse

1  „Schneller" gibt es nicht ohne Angabe der Garantie.
   HSQLDB bestätigt einen Commit, bevor geschrieben wurde.
   PostgreSQL bestätigt erst, wenn geschrieben wurde.

2  Der stärkste Hebel liegt in der Anwendung — und kostet keine Sicherheit.
   Batch statt Einzel-Commit bringt Faktor 9 bis 20,5, ohne eine einzige
   bestätigte Zeile zu riskieren. Der Wechsel des Datenbanksystems
   bringt Faktor 0,5 bis 4,7.

   Wer bündelt, muss die Garantie nicht aufgeben:
   im Batch-Betrieb kostet sie zwischen -9 % und +10 %.

3  Jedes System hat eine Domäne, in der es gewinnt.
   PostgreSQL: komplexe Abfragen über mehrere Tabellen.
   HSQLDB: viele einfache Zugriffe ohne Serverweg.

4  Geschwindigkeit und Vorhersagbarkeit sind Gegensätze.
   Die schnellste Konfiguration schwankt um bis zu 37 %,
   die langsamste bleibt unter 2 %.

5  HSQLDB synchron ist nicht plattformunabhängig.
   Unter Windows war sie mit 18,44 s die langsamste aller Konfigurationen.

Empfehlung nach Einsatzzweck

Testumgebung und automatisierte Tests        HSQLDB asynchron
  Millionen von Operationen ohne echte Dauerhaftigkeit. Es genügt, das
  Speichern und die Korrektheit zu simulieren. Kein Server, keine
  Installation, ein JAR — der Testlauf startet in Sekunden.
  Gemessener Vorteil: 11x schneller beim Schreiben.

Produktivbetrieb des Onlineshops             PostgreSQL synchron
  Bestelldaten dürfen bei einem Absturz nicht verloren gehen.
  PostgreSQL bestätigt erst nach der Sicherung, liest komplexe Abfragen
  schneller und liefert die stabilsten Messwerte.

In beiden Fällen gilt: Commits bündeln.
```

---

### Folie 21 — Ausblick

```
Was wir beim nächsten Mal anders machen

Symmetrische asynchrone Messung: PostgreSQL ohne UNLOGGED,
  dann unterscheiden sich die Konfigurationen in genau einer Einstellung.
HSQLDB zusätzlich im Serverbetrieb messen — damit ließe sich der Preis
  des Serverzugriffs direkt bestimmen statt indirekt aus Batch und Einzel.
TRUNCATE statt DELETE zwischen den Durchläufen.

Offene Fragen
Wie verhalten sich beide Systeme bei 10, 50 und 100 parallelen Verbindungen?
Wie viele bestätigte Transaktionen verliert HSQLDB asynchron bei einem
  Stromausfall wirklich? Das ist der eigentliche Preis der 11x.
```

Последний пункт — сильный финал: вы понимаете, что именно купили за скорость.

---

### Folie 22 — Dank und Fragen

Без изменений.

---

### Folie 23 — Quellenverzeichnis

Добавить версии и четыре источника, на которых держится новая аргументация. Дат обращения не ставим.

```
Eingesetzte Versionen
Java 22 · HSQLDB 2.7.4 · PostgreSQL-Server 18.4
PostgreSQL JDBC 42.7.3 · JUnit Jupiter 5.10.2

Werkzeuge
IntelliJ IDEA · Visual Studio Code · Apache NetBeans · Claude Code
Code-Repository: https://github.com/IlyaBashlyaev/DatabaseBenchmark

Fachquellen
HSQLDB User Guide, „System Management and Deployment" — hsqldb.write_delay
  https://hsqldb.org/doc/2.0/guide/
PostgreSQL, „Reliability and the Write-Ahead Log"
  https://www.postgresql.org/docs/current/wal.html
PostgreSQL, Parameter synchronous_commit
  https://www.postgresql.org/docs/current/runtime-config-wal.html
PostgreSQL, „Unlogged Tables" in CREATE TABLE
  https://www.postgresql.org/docs/current/sql-createtable.html
```

Без этих ссылок утверждения про поведение коммита — ваше слово против слова преподавателя. С ними — задокументированное поведение продукта.

---

## 7. Оформление

### Палитра

Цветные квадратики в оглавлении, картинку со слайда 3, оранжево-жёлтые таблицы и тёмные скриншоты кода убираем.

| Роль | Цвет |
|---|---|
| Фон | белый, подложки `#F7F9FC` |
| Заголовки и текст | `#1F3864` |
| Второстепенный текст | `#5B6B82` |
| MacBook Pro M1 | `#9DC3E6` |
| Lenovo ThinkPad | `#2E75B6` |
| MacBook Pro A2141 | `#1F3864` |
| Акцент — только аномалия | `#C00000` |

Одна трёхступенчатая шкала одного тона для железа. СУБД различаются подписью и позицией, отдельных цветов им не нужно.

### Правила слайда

- Не больше 8 коротких блоков, читается за несколько секунд.
- Вся статистика — графиками. Цифры в тексте только там, где без них не понять.
- Никаких целых предложений, кроме одной итоговой строки внизу.
- Шрифт от 20 pt в тексте, от 28 pt в заголовках.
- Номер слайда на каждом.
- У каждой диаграммы подпись и единица измерения.
- Немецкий формат чисел: запятая как десятичный разделитель.
- Настоящие умлауты `ä ö ü ß`, а не `ae oe ue ss`. Это касается и подписей на графиках.
- Никаких тёмных фонов и белого текста на тёмном: через проектор днём не читается. Акцент даём светлой плашкой с цветной полосой слева.
- Единицы измерения на графиках стоят у самой оси (бейдж `Sekunden` / `Millisek.`) и повторяются в каждой подписи деления.
- Скриншоты кода — светлая тема IDE.

### Готовые файлы

| Файл | Для слайда |
|---|---|
| `docs/results/charts/schreiben.svg` | 13 |
| `docs/results/charts/lesen.svg` | 14 |
| `docs/er_diagram/er_diagram.svg` | 6 |
| `docs/class_diagram/simple/class_diagram.svg` | 9 |
| `docs/results/charts/ergebnisse.html` | предпросмотр обоих графиков |

---

## 8. Исправить в тексте

| Где | Было | Стало |
|---|---|---|
| Слайды 16, 17, 18, 20 (v2) | `HSLQDB` | `HSQLDB` |
| Слайд 8 (v2) | `Schwakungen` | `Schwankungen` |
| `docs/results/hsqldb/*.md` | `Schriebweise` | `Schreibweise` |
| Слайд 9 (v2) | `Lenovo Thinkpad T422` | `Lenovo ThinkPad P16 Gen 1` |
| Слайд 8 (v2) | `Pro eine Suboperation: 10.000 Datensätze` | 40.000 Zeilen je Messung, 10.000 je Tabelle |
| Везде | `ue`, `ae`, `oe`, `ss` | `ü`, `ä`, `ö`, `ß` — везде настоящие умлауты |

---

## 9. Закрытые вопросы

| Вопрос | Ответ |
|---|---|
| Модель ThinkPad | Lenovo ThinkPad P16 Gen 1 |
| Версия сервера PostgreSQL | 18.4 |
| Число строк в SELECT | 3 запроса × 10 000 строк × 10 повторов = 300 000 на операцию. Проверено по столбцу `Rows` в CSV |
| Дата презентации | 23.09.2026 |
| Даты обращения к источникам | не нужны, из списка убраны |

Открытых вопросов не осталось.

---

## 10. Порядок работы

1. Переставить слайды в порядок из раздела 5.
2. Внести готовый текст по разделу 6.
3. Вставить четыре готовых SVG.
4. Добавить новые слайды 5, 15, 16, 17, 18, 19, 21.
5. Удалить слайд суммарной длительности (v2, слайд 17).
6. Применить палитру и правила раздела 7, перезалить скриншоты кода в светлой теме.
7. Исправить опечатки по разделу 8.
8. Закрыть пункты раздела 9.
9. Прогон по времени.

---

## 11. Замечания к оформлению после первой сборки

Правки, внесённые после просмотра колоды на экране:

1. **Тёмные слайды убраны полностью.** Титульный, «Erkenntnisse» и «Vielen Dank» были тёмно-синими с белым текстом — через проектор днём это не читается. Все три переведены на светлый фон. Акценты теперь дают светлой плашкой `#E4ECF5` с цветной полосой слева.
2. **Контраст поднят.** Второстепенный текст был `#5B6B82`, стал `#3D4F66`. Линии сетки на графиках и подписи в диаграммах тоже темнее.
3. **Диаграмма классов**: шрифт увеличен, блоки расширены, чтобы подписи не налезали на имена классов.
4. **Единицы измерения** вынесены к самой оси: бейдж `Sekunden` / `Millisek.` плюс единица в каждой подписи деления. Раньше их можно было узнать только из подзаголовка.
5. **Графики перегенерированы** в 4200 px по ширине вместо 2600 — на слайде это около 340 dpi вместо 210.
6. **Блок `Zielgruppe`** со слайда 3 убран.
7. **Слайд 18**: строка `widerlegt · gleichmäßige Ergebnisse` удалена. Такой гипотезы мы не выдвигали, значит и опровергнуть её не можем. Разброс упомянут как отдельный вывод на слайде 20.
8. **Реальные скриншоты** дерева проекта и четырёх фрагментов кода из IntelliJ IDEA вместо набранного текста.
9. **IntelliJ IDEA** добавлена в `Werkzeuge`.

### Что стоит переснять

Скриншоты сделаны в масштабе 1× (72 dpi): дерево 277 × 455 px, самый широкий фрагмент кода 837 × 48 px. На слайде они читаются, но мягче остальной графики. Чтобы стали резкими — переснять на Retina-экране или увеличить шрифт редактора в IntelliJ (`Cmd` + `+`) перед снимком. Файлы подменяются в презентации без изменения вёрстки.
