# Sprechertext — DB-Benchmark HSQLDB und PostgreSQL

Präsentation vom 23.09.2026 · 23 Folien · rund 20 Minuten

Текст для произнесения, не для слайдов. Слайды дают числа и картинки, речь даёт смысл и связки. Нигде не пересказываем слайд дословно.

Общая длительность 20 мин 40 с.

---

## Folie 1 — Deckblatt · 20 s

> Guten Tag. Wir stellen Ihnen heute unser Blockwochenprojekt vor: einen Benchmark zweier Datenbanksysteme, HSQLDB und PostgreSQL.
>
> Die eigentliche Frage steht im Untertitel: Was kostet die Sicherheit einer Transaktion? Und wir sagen Ihnen gleich zu Beginn: Unsere ursprüngliche Erwartung hat sich nicht bestätigt. Genau das ist der interessante Teil.

Последнее предложение — обещание интриги. Произнести спокойно, без извинения в голосе.

---

## Folie 2 — Agenda · 15 s

> Wir gehen in vier Schritten vor. Zuerst die Fragestellung und unsere Erwartung. Dann das Testszenario und das Messverfahren. Danach die Ergebnisse. Und zum Schluss die Auswertung und das, was wir daraus gelernt haben.

---

## Folie 3 — Ausgangssituation und Fragestellung · 60 s

> Stellen Sie sich einen Onlineshop vor. Kunden, Produkte, Rechnungen, Rechnungspositionen. Diese Daten müssen gespeichert werden, und zwar zuverlässig.
>
> Für die Speicherung gibt es zwei grundsätzlich verschiedene Wege. Entweder eine eingebettete Datenbank, die direkt in der Anwendung läuft. Oder ein Datenbankserver, der als eigener Dienst arbeitet.
>
> Unsere Zielgruppe ist ein Entwicklungsteam, das genau diese Entscheidung treffen muss.
>
> Die Hauptfrage lautet deshalb nicht einfach „welche Datenbank ist schneller". Sie lautet: Welches System eignet sich wofür — und was kostet die Zusage, dass eine Transaktion wirklich gespeichert ist?
>
> Dazu kommen drei Teilfragen: Wie stark wirkt das Transaktionsmodell? Wie wirkt Denormalisierung beim Lesen? Und wie stark hängt alles von der Hardware ab?

---

## Folie 4 — Die beiden Datenbanksysteme · 60 s

> Bevor wir eine Erwartung formulieren können, müssen wir die beiden Systeme kennen.
>
> HSQLDB läuft eingebettet. Sie ist eine einzige JAR-Datei, sie startet mit der Anwendung, und sie speichert ihre Daten in einem Verzeichnis. Es gibt keinen Serverdienst und keinen Netzwerkweg. Und — das ist der entscheidende Punkt — im Standardbetrieb bestätigt sie einen Commit, bevor die Daten auf der Festplatte liegen.
>
> PostgreSQL ist das Gegenteil. Ein eigener Serverdienst, jeder Zugriff läuft über diesen Dienst. Dafür bekommt man Skalierbarkeit, Stabilität und sehr starke SQL-Funktionen. Der Preis ist ein aufwendigeres Setup und ein Weg über den Server bei jedem einzelnen Zugriff.
>
> Merken Sie sich bitte diese beiden Punkte: kein Serverweg, und Commit ohne Warten. Aus ihnen ergibt sich unsere Hypothese.

---

## Folie 5 — Erwartung · 50 s

> Unsere Hypothese vor der Messung war klar formuliert, und wir waren uns ziemlich sicher.
>
> Wir haben angenommen: HSQLDB arbeitet im Arbeitsspeicher und ist deshalb in allen Operationen schneller als PostgreSQL.
>
> Die Begründung schien schlüssig. Kein Netzwerk, kein Verbindungsaufbau, eine schlanke Architektur. Wir haben erwartet, dass HSQLDB beim Schreiben gewinnt, beim Lesen gewinnt, und zwar auf jeder Hardware.
>
> Ob das stimmt, entscheidet die Messung. So viel sei verraten: die andere Hälfte hat uns überrascht — und die Begründung, die Sie hier lesen, war sogar falsch. Warum, zeigen wir ab Folie 14.

Пауза после «zur Hälfte bestätigt». Это крючок на всю оставшуюся презентацию.

---

## Folie 6 — Szenario: Onlineshop · 50 s

> Das ist unser Datenmodell. Links die normalisierte Variante: vier Tabellen. Ein Kunde erhält Rechnungen, eine Rechnung enthält Positionen, jede Position verweist auf ein Produkt. Jedes Datum steht genau einmal, Fremdschlüssel sichern die Integrität.
>
> Rechts die denormalisierte Variante. Eine einzige Tabelle, FLAT_SALES. Die farbigen Balken zeigen, woher jede Spalte stammt — aus allen vier Tabellen. Kundenname, Rechnungssumme, Produktname und Menge stehen in jeder Zeile zusammen.
>
> Wichtig: FLAT_SALES hat keine Fremdschlüssel. Sie ist eine abgeleitete Tabelle, kein eigenständiger Teil des Modells. Beide Datenbanksysteme bekommen exakt dieses Schema.

---

## Folie 7 — Was gemessen wird · 60 s

> Gemessen haben wir zwei Grundoperationen: Schreiben und Lesen.
>
> Dagegen halten wir zwei Optimierungen, die ein Entwickler tatsächlich einsetzen würde. Beim Schreiben das Bündeln: statt jede Zeile einzeln zu bestätigen, fassen wir tausend Zeilen zu einem Paket zusammen. Beim Lesen die Denormalisierung: statt über vier Tabellen zu verknüpfen, lesen wir aus der einen flachen Tabelle.
>
> Daraus ergeben sich vier Messoperationen. INSERT_SINGLE — jede Zeile einzeln. INSERT_BATCH — Pakete zu tausend Zeilen. SELECT_JOIN — drei Abfragen über bis zu vier verknüpfte Tabellen. Und SELECT_DENORM — dieselben drei Abfragen auf FLAT_SALES.
>
> Dieselben drei Abfragen, betone ich: gleiche Spalten, gleiche Gruppierung, gleiche Sortierung. Nur das FROM unterscheidet sich.

---

## Folie 8 — Die zwei Schreibmodi · 75 s

> Diese Folie ist die wichtigste der ersten Hälfte.
>
> Beide Datenbanksysteme lassen sich in zwei Betriebsarten fahren. Wir nennen sie synchron und asynchron.
>
> Synchron heißt: Der Commit wartet, bis die Daten tatsächlich auf der Festplatte sind. Erst dann kehrt er zur Anwendung zurück. Bei HSQLDB stellen wir dafür WRITE DELAY auf FALSE, bei PostgreSQL ist es die Standardeinstellung mit dem Write-Ahead-Log.
>
> Asynchron heißt: Der Commit wird bestätigt, bevor geschrieben wurde. HSQLDB sammelt die Änderungen und schreibt alle fünfhundert Millisekunden. Bei PostgreSQL haben wir synchronous_commit abgeschaltet und die Tabellen als UNLOGGED angelegt.
>
> Und jetzt der Punkt, auf den es ankommt: Vergleichbar sind nur Konfigurationen derselben Spalte. Die linke Spalte gibt eine Zusage. Die rechte Spalte gibt ein Versprechen. Wer die beiden mischt, vergleicht nicht zwei Datenbanken, sondern zwei verschiedene Garantien.

Это узловой слайд. Не торопиться. Последний абзац — медленно.

---

## Folie 9 — Das Benchmark-Programm · 55 s

> Das ist unser Messprogramm, links die Projektstruktur, rechts die Klassen.
>
> Drei Packages. Im Package benchmark liegt die Messlogik und die Erzeugung der Testdaten. Im Package dao die JDBC-Verbindung. Im Package util alles rund um Statistik, CSV-Protokoll und Systemdaten. Die Klasse DatabaseBenchmark steuert alle vier Konfigurationen nacheinander.
>
> Zwei Klassen fallen auf: Customer und Product. Die stammen aus der JDBC-Übung der ersten beiden Blockwochentage und gehören bewusst nicht zum Messpfad.
>
> Denn gemessen wird ausschließlich die Zeit zwischen Absenden und Rückkehr des SQL-Aufrufs. Kein Objekt-Mapping, keine Datenerzeugung. Sonst würden wir die Geschwindigkeit der JVM messen und nicht die der Datenbank.

Последний абзац снимает вопрос про DAO до того, как его зададут.

---

## Folie 10 — Code: Messung und Modus · 55 s

> Hier sehen Sie, wie genau gemessen wird. Der Zeitnehmer umschließt nur den SQL-Aufruf: nanoTime davor, executeBatch und commit, nanoTime danach. Alles andere liegt außerhalb.
>
> Darunter die Umschaltung der Betriebsarten. Vier Konfigurationen, und sie unterscheiden sich in genau zwei Einstellungen: WRITE DELAY bei HSQLDB und synchronous_commit bei PostgreSQL.
>
> Das ist bewusst so klein gehalten. Je weniger sich zwischen zwei Messungen ändert, desto eindeutiger ist die Ursache eines Unterschieds.

---

## Folie 11 — Code: Testdaten · 45 s

> Die Testdaten erzeugen wir selbst. Namen entstehen aus festen Wortlisten, Adressen aus Hausnummer, Straße und Stadt, Produktnamen aus Adjektiv und Substantiv. Preise, Kosten und Mengen werden zufällig berechnet.
>
> Entscheidend ist die erste Zeile: der Zufallsgenerator hat einen festen Startwert, die Zahl zweiundvierzig. Dadurch erzeugt jeder Lauf auf jedem Rechner exakt dieselben Daten.
>
> Das heißt: Unterschiede in den Messwerten können nicht von den Daten kommen. Sie kommen von der Datenbank oder von der Hardware.

---

## Folie 12 — Testrechner, Umfang und Reproduzierbarkeit · 70 s

> Gemessen haben wir auf drei Rechnern: einem MacBook Pro mit Apple M1 Pro, einem Lenovo ThinkPad P16 mit Intel Core i7 der zwölften Generation unter Windows, und einem älteren MacBook Pro von 2019.
>
> Der Umfang je Messung: zehntausend Zeilen in jede der vier Tabellen, zusammen vierzigtausend Zeilen. Bei INSERT_SINGLE ergibt das vierzigtausend Commits. Bei INSERT_BATCH nur vierzig. Beim Lesen drei Abfragen zu je zehntausend Zeilen.
>
> Jede Operation haben wir zehnmal wiederholt. Insgesamt kommen wir auf fünf Komma sechs Millionen Zeilen.
>
> Warum zehnmal? Wiederholungen unterdrücken zufällige Messabweichungen und erlauben uns, Mittelwert und Standardabweichung zu berechnen. Und die drei Rechner zeigen, ob ein Ergebnis an der Datenbank liegt oder an der Hardware.
>
> Ein Wert vorab, der später wichtig wird: PostgreSQL im synchronen Betrieb schwankt um weniger als sieben Prozent. HSQLDB im asynchronen Betrieb schwankt um bis zu siebenunddreißig Prozent.

---

## Folie 13 — Ergebnisse: Schreibzugriffe · 75 s

> Jetzt die Ergebnisse. Links das Schreiben mit einzelnen Commits, rechts mit Paketen. In jedem Bereich stehen links die beiden synchronen Konfigurationen, rechts die beiden asynchronen. Die drei Balken sind die drei Rechner, von hell nach dunkel.
>
> Bitte beachten Sie: die Skala ist logarithmisch. Zwischen der schnellsten und der langsamsten Messung liegt der Faktor hundertdreiunddreißig — linear wäre die schnellste Säule nicht mehr sichtbar.
>
> Links sehen Sie unsere Hypothese bestätigt. HSQLDB asynchron braucht auf dem M1 hundertvierzig Millisekunden, PostgreSQL synchron über sechs Sekunden. Das sind mehr als vierzig mal.
>
> Rechts passiert etwas anderes. Sobald wir bündeln, schrumpft alles zusammen. Die Garantie kostet im Batch-Betrieb fast nichts mehr.
>
> Und ein Wert fällt heraus: der rote. HSQLDB synchron auf dem ThinkPad, achtzehn Komma vier Sekunden. Dazu kommen wir gleich.

Красное значение — не объяснять сейчас, только указать. Объяснение на слайде 17.

---

## Folie 14 — Ergebnisse: Lesezugriffe · 75 s

> Beim Lesen bricht unsere Hypothese.
>
> Links die Abfragen mit JOIN über vier Tabellen. Hier gewinnt PostgreSQL — auf allen drei Rechnern, in beiden Betriebsarten. Auf dem M1 mit knapp neunundzwanzig gegen dreiundsiebzig Millisekunden, also zweieinhalb mal schneller.
>
> Rechts dieselben Abfragen auf der flachen Tabelle. Hier gewinnt HSQLDB, ebenfalls auf allen drei Rechnern.
>
> Die Erklärung ist sauber trennbar. Bei einer komplexen Abfrage entscheidet der Abfrageoptimierer, und der von PostgreSQL ist deutlich stärker. Der Weg über den Server fällt dabei nicht mehr ins Gewicht. Bei einer trivialen Abfrage gibt es dagegen kaum Arbeit zu verteilen — dann dominiert genau dieser Weg, und HSQLDB gewinnt.
>
> Noch etwas: Vergleichen Sie die linke und die rechte Gruppe in jedem Bereich. Sie sind praktisch identisch. Der Schreibmodus hat auf das Lesen keinen Einfluss — erwartbar, aber schön, es bestätigt zu sehen.

---

## Folie 15 — Drei Hebel · 75 s

> Wir haben uns gefragt: Wenn man eine langsame Anwendung schneller machen will — woran soll man drehen?
>
> Dafür nehmen wir die schlechteste Konfiguration als Startpunkt: PostgreSQL synchron mit einzelnen Commits. Und dann ändern wir jeweils genau eine Sache.
>
> Bündeln statt einzeln bestätigen bringt Faktor neun bis zwanzig Komma fünf. Ein anderes Datenbanksystem bringt Faktor null Komma fünf bis vier Komma sieben — auf dem ThinkPad wird es sogar langsamer. Die Garantie aufzugeben bringt Faktor zwei.
>
> Das Ergebnis ist eindeutig: Der stärkste Hebel liegt in der Anwendung, nicht im Datenbankprodukt.
>
> Die Gegenprobe unten zeigt, warum. Bei vierzigtausend Anweisungen liegen HSQLDB und PostgreSQL rund siebenundsechzig Mikrosekunden pro Zeile auseinander. Bei vierzig Anweisungen nur noch knapp vier. Den Abstand erzeugt der Aufwand je Anweisung, nicht die Arbeit an den Daten.

Если спросят, из чего состоят эти 67 µs: честный ответ — замер не разделяет круговой обмен с сервером и обслуживание транзакции, они меняются вместе. Для разделения нужен был бы прогон HSQLDB в серверном режиме.

---

## Folie 16 — Geschwindigkeit gegen Sicherheit · 80 s

> Jeder dieser Hebel hat einen Preis. Diese Folie stellt beides nebeneinander: was man gewinnt und was man im Fehlerfall verliert.
>
> Bündeln bringt neun bis zwanzig mal — und kostet keine einzige bestätigte Zeile. Ein Absturz mitten im Paket verwirft höchstens neunhundertneunundneunzig Zeilen, die noch gar nicht bestätigt waren.
>
> HSQLDB asynchron bringt etwa acht bis zwölf mal. Der Preis: bis zu fünfhundert Millisekunden bereits bestätigter Transaktionen. Bei unserer Schreibrate sind das rund hundertvierzigtausend Zeilen.
>
> PostgreSQL mit abgeschaltetem synchronous_commit bringt gut zwei mal und kostet die letzten paar hundert Millisekunden, rund achttausendfünfhundert Zeilen.
>
> UNLOGGED-Tabellen sind der gefährlichste Eintrag. Wir können den Gewinn nicht einmal beziffern, weil wir ihn nicht getrennt gemessen haben — aber bei einem Absturz verliert PostgreSQL die gesamte Tabelle.
>
> Zwei Sätze bleiben von dieser Folie. Erstens: Der stärkste Hebel ist der einzige, der nichts kostet. Und zweitens: Wer bündelt, braucht das Risiko gar nicht. Im Batch-Betrieb kostet die volle Garantie zwischen minus neun und plus zehn Prozent. Fünf von sechs Konfigurationen. Die sechste ist die Ausnahme auf der nächsten Folie.

Последняя фраза — переход. Произнести как подводку, не как конец мысли.

---

## Folie 17 — Die Windows-Anomalie · 65 s

> Die Ausnahme ist HSQLDB im synchronen Betrieb auf dem ThinkPad: achtzehn Komma vier Sekunden. Die einzige Konfiguration im ganzen Projekt, in der HSQLDB das schlechteste Ergebnis liefert.
>
> Wir haben drei Belege gesucht, und zwar in unseren eigenen Daten.
>
> Erstens: An der Hardware liegt es nicht. PostgreSQL läuft auf demselben ThinkPad mit neun Komma vier Sekunden — doppelt so schnell wie auf dem älteren MacBook.
>
> Zweitens: Gezahlt wird je Commit. Im Batch-Betrieb beträgt der Abstand auf demselben Rechner nur das Eins-Komma-Acht-fache.
>
> Drittens: Die Standardabweichung liegt bei vierundzwanzig Prozent gegen dreieinhalb auf dem M1. So sieht keine stabile Kosten aus, so sieht eine äußere Störung aus.
>
> Unsere wahrscheinliche Erklärung: Unter Windows leert der Systemaufruf den Cache des Laufwerks wirklich, und zusätzlich prüft der Virenscanner jeden Schreibvorgang. Unter macOS schiebt der entsprechende Aufruf die Daten nur bis zum Controller.
>
> Das heißt aber auch: HSQLDB synchron gibt unter macOS eine schwächere Zusage als unter Windows — unter demselben Namen.

---

## Folie 18 — Hypothese und Ergebnis · 60 s

> Zurück zu unserer Hypothese. Wir hatten erwartet, HSQLDB sei in allen Operationen schneller.
>
> Bestätigt: beim Schreiben ohne Garantie, elf mal. Und beim einfachen, denormalisierten Lesen.
>
> Teilweise bestätigt: beim Schreiben mit Garantie. Auf macOS ist HSQLDB schneller, unter Windows langsamer.
>
> Widerlegt: beim Lesen mit JOIN, da gewinnt PostgreSQL überall.
>
> Und jetzt der eigentliche Punkt. Nicht die Speichertechnik war der Unterschied, sondern der Zeitpunkt der Commit-Bestätigung. Beide Systeme schreiben auf die Festplatte. HSQLDB wartet im Standardbetrieb nur nicht darauf.
>
> Unsere Hypothese war also nicht nur im Ergebnis falsch, sondern auch in der Begründung. Gefunden haben wir das erst durch die Messung.

Последнее предложение — ключевое. Произнести ровно, как факт, а не как извинение.

---

## Folie 19 — Grenzen des Vergleichs · 60 s

> Wir wollten zwei Systeme unter identischen Bedingungen vergleichen. Das ist uns nicht vollständig gelungen, und das sagen wir offen.
>
> Identisch waren: der Code, das Schema, die Datenmenge, die Wiederholungen, das Messfenster, die Rechner und sogar der Zufalls-Startwert.
>
> Nicht identisch war dreierlei. PostgreSQL nutzt im asynchronen Betrieb UNLOGGED-Tabellen, führt also gar kein Journal — die Zusage ist dort schwächer als bei HSQLDB. Zwischen den Durchläufen haben wir DELETE statt TRUNCATE verwendet. Und HSQLDB ist eingebettet, PostgreSQL ist ein Serverdienst — das lässt sich nicht gleichsetzen, ohne eines der Produkte zu verfälschen.
>
> Warum das die Ergebnisse trotzdem nicht entwertet: PostgreSQL hatte in der asynchronen Messung die schwächere Zusage. Der Rückstand kann also nicht daran liegen, dass es trotzdem geschrieben hätte. Was bleibt, ist der Aufwand je Anweisung — und genau den zeigt Folie fünfzehn.

---

## Folie 20 — Erkenntnisse und Empfehlung · 80 s

> Fünf Erkenntnisse.
>
> Erstens: „Schneller" gibt es nicht ohne Angabe der Garantie. HSQLDB bestätigt einen Commit, bevor geschrieben wurde, PostgreSQL erst danach.
>
> Zweitens: Der stärkste Hebel liegt in der Anwendung — und er kostet keine Sicherheit.
>
> Drittens: Jedes System hat eine Domäne, in der es gewinnt. PostgreSQL bei komplexen Abfragen, HSQLDB bei vielen einfachen Zugriffen.
>
> Viertens: Geschwindigkeit und Vorhersagbarkeit sind Gegensätze. Die schnellste Konfiguration schwankt um siebenunddreißig Prozent, die langsamste bleibt unter zwei.
>
> Fünftens: HSQLDB synchron ist nicht plattformunabhängig.
>
> Daraus zwei Empfehlungen. Für Testumgebungen HSQLDB asynchron: Millionen von Operationen, ohne dass echte Dauerhaftigkeit nötig wäre. Es genügt, das Speichern zu simulieren. Kein Server, keine Installation, eine JAR-Datei.
>
> Für den Produktivbetrieb des Onlineshops PostgreSQL synchron: Bestelldaten dürfen bei einem Absturz nicht verloren gehen, komplexe Abfragen sind schneller, und die Messwerte sind die stabilsten von allen.
>
> Und in beiden Fällen gilt: Commits bündeln.

---

## Folie 21 — Ausblick · 45 s

> Was wir beim nächsten Mal anders machen würden.
>
> Die asynchrone Messung symmetrisch aufbauen, also PostgreSQL ohne UNLOGGED. Dann unterscheiden sich die Konfigurationen in genau einer Einstellung.
>
> HSQLDB zusätzlich im Serverbetrieb messen. Damit ließe sich der Preis des Serverzugriffs direkt bestimmen statt indirekt.
>
> Und zwei offene Fragen bleiben. Wie verhalten sich beide Systeme bei parallelen Verbindungen? Und wie viele bestätigte Transaktionen verliert HSQLDB asynchron bei einem echten Stromausfall? Das ist der eigentliche Preis der elf mal.

---

## Folie 22 — Dank und Fragen · 10 s

> Damit sind wir am Ende. Vielen Dank für Ihre Aufmerksamkeit — wir beantworten gerne Ihre Fragen.

---

## Folie 23 — Quellenverzeichnis · 10 s

Показывается при вопросах. Отдельного текста не требует.

---

## Подготовка к вопросам

Вероятные вопросы и короткие ответы. Наизусть учить не надо, важно знать направление.

**«Warum ist HSQLDB synchron schneller als PostgreSQL synchron, wenn beide auf die Platte warten?»**
> Weil HSQLDB eingebettet läuft. Ein SQL-Aufruf ist dort ein Methodenaufruf in derselben JVM, bei PostgreSQL ein Weg über den Serverdienst. Auf dem ThinkPad kehrt sich der Vorteil aber um — siehe Folie siebzehn.

**«Sie sagen, HSQLDB arbeite nicht im Arbeitsspeicher. Was denn dann?»**
> Wir legen CACHED-Tabellen an. Die Zeilen liegen in einer Datei, im Arbeitsspeicher ist nur ein Cache. HSQLDB ist hier eine Dateidatenbank, die in den Prozess eingebettet ist.

**«Ist der Vergleich JOIN gegen denormalisiert fair?»**
> Die drei Abfragen sind in beiden Varianten identisch aufgebaut — gleiche Spalten, gleiche Gruppierung, gleiche Sortierung, nur das FROM unterscheidet sich. Die flache Tabelle hat allerdings zehntausend Zeilen, der JOIN geht über vierzigtausend. Weniger Daten ist Teil des Verfahrens. Und: Der Aufbau der flachen Tabelle ist nicht mitgemessen.

**«Warum haben Sie keine DAO-Klassen für Invoice und Item?»**
> Weil die Messung bewusst ohne Objekt-Mapping arbeitet. Customer und Product stammen aus der JDBC-Übung der Blockwoche und liegen außerhalb des Messpfads.

**«Wie viel Prozent Unsicherheit haben Ihre Zahlen?»**
> Je nach Konfiguration ein Komma sieben bis siebenunddreißig Prozent relative Standardabweichung, bei INSERT_SINGLE. Die genauen Werte stehen auf Folie zwölf.

**«Hätten Sie nicht einfach neu messen sollen?»**
> Das haben wir erwogen. Ein vollständiger Lauf dauert knapp vier Minuten. Wir haben uns bewusst dagegen entschieden, weil die vorhandenen Daten die Fragen beantworten und wir die Abweichungen offenlegen können, statt sie zu kaschieren. Was ein neuer Lauf zusätzlich bringen würde, steht auf Folie einundzwanzig.

---

## Тайминг

| Folien | Блок | Время |
|---|---|---|
| 1 – 8 | постановка задачи, системы, гипотеза, сценарий | 6 мин 30 с |
| 9 – 14 | программа, объём, результаты | 6 мин 15 с |
| 15 – 23 | объяснение, границы, выводы | 7 мин 55 с |
| | **Итого** | **20 мин 40 с** |

Если нужно уложиться в 15 минут — сокращать слайды 10, 11 и 21, они наименее нагружены содержанием. Слайды 8, 16 и 18 не трогать, на них держится вся линия.
