# Testkonzept – Datenintegrität (Person C)

Entwurf der Kapitel 3.3 und 5.3 für das Testkonzept LB2, nach `docs/PLAN-LB2.md` §5.3. Wird in das Word-Dokument übernommen. Die Ausführung (Tester, Datum, Mängelklasse, Mängelbeschreibung) wird nach dem Testlauf ergänzt.

---

## 3.3 Datenintegrität – Begründung

Der Speed Reader berechnet aus den gespeicherten Lesesitzungen die durchschnittliche und die empfohlene Lesegeschwindigkeit (WPM). Diese Statistiken sind nur so verlässlich wie die Daten, auf denen sie beruhen:

- **Korrekte Beziehungen:** Jede Lesesitzung und jedes Lesezeichen gehört zu genau einem Benutzer und einem Text. Eine Sitzung ohne gültigen Benutzer oder ein verwaister Datensatz nach dem Löschen eines Benutzers verfälscht die Statistik oder führt zu Fehlern beim Laden.
- **Eindeutigkeit:** Pro Benutzer und Text darf es nur ein Lesezeichen geben. Gäbe es zwei, wäre unklar, an welcher Stelle der Benutzer weiterlesen soll.
- **Unveränderte Daten:** Texte enthalten Umlaute und Sonderzeichen, können sehr lang sein, und das Datum einer Sitzung bestimmt, welche Sitzungen als «die neusten» gelten. Kommen die Daten verändert aus der Datenbank zurück, stimmen Anzeige und Statistik nicht mehr.
- **Gültige Werte:** Die Anwendung prüft WPM und Verständnis-Score bereits im Service. Die Datenbank ist aber die letzte Verteidigungslinie: Daten, die am Service vorbei geschrieben werden (Skripte, Importe), dürfen die fachlichen Grenzen ebenfalls nicht verletzen.

Diese Regeln lassen sich nur gegen eine echte Datenbank prüfen. Die Unit-Tests aus LB1 verwenden Mocks und können keine Fremdschlüssel, Unique-Constraints oder Zeichensätze testen.

---

## 5.3 Datenintegrität – Testfälle

**In-Scope:** Fremdschlüssel, Unique-Constraint, NOT NULL, Löschverhalten bei abhängigen Datensätzen, unveränderte Rückgabe von Text, langem Inhalt und Zeitstempel, Wertebereiche in der Datenbank.

**Out-of-Scope:**

- Datenvalidierung im Service – bereits durch die Unit-Tests aus LB1 abgedeckt.
- Sicherheit (SQL-Injection, Berechtigungen) – Spring Data verwendet Prepared Statements; nicht Schwerpunkt dieses Testkonzepts.
- Backup und Wiederherstellung – Aufgabe des Betriebs, nicht unseres Codes.

Alle Tests laufen in der Klasse `DataIntegrityIT` gegen eine echte MySQL-Datenbank (Testcontainers). Vor jedem Test werden alle Tabellen geleert.

### T-021 – Fremdschlüssel: Session ohne gültigen User

| | |
|---|---|
| **ID / Bezeichnung** | T-021 – Fremdschlüssel: Session ohne gültigen User |
| **Beschreibung** | Eine Lesesitzung darf nur für einen existierenden Benutzer gespeichert werden. |
| **Testvoraussetzung** | Nur ein Text ist in der Datenbank, kein Benutzer. |
| **Testschritte** | 1. Per SQL eine Session mit `user_id = 999` einfügen.<br>2. Anzahl Zeilen in `reading_sessions` abfragen. |
| **Erwartetes Ergebnis** | Die Datenbank lehnt das Einfügen ab (Fremdschlüssel-Verletzung). `reading_sessions` enthält 0 Zeilen. |
| **Tester / Datum** | |
| **Mängelklasse** | |
| **Mängelbeschreibung** | |

### T-022 – User mit Sessions löschen

| | |
|---|---|
| **ID / Bezeichnung** | T-022 – User mit Sessions löschen |
| **Beschreibung** | Ein Benutzer, zu dem noch Lesesitzungen existieren, darf nicht gelöscht werden, damit keine verwaisten Sitzungen entstehen. |
| **Testvoraussetzung** | Ein Benutzer mit einer Lesesitzung ist in der Datenbank. |
| **Testschritte** | 1. `users.deleteById(userId)` aufrufen.<br>2. Benutzer und Sitzung in der Datenbank abfragen. |
| **Erwartetes Ergebnis** | Die Datenbank lehnt das Löschen ab (`DataIntegrityViolationException`). Benutzer und Sitzung sind weiterhin vorhanden. |
| **Tester / Datum** | |
| **Mängelklasse** | |
| **Mängelbeschreibung** | |

### T-023 – Eindeutiges Bookmark pro User und Text

| | |
|---|---|
| **ID / Bezeichnung** | T-023 – Eindeutiges Bookmark pro User und Text |
| **Beschreibung** | Pro Kombination aus Benutzer und Text darf nur ein Lesezeichen existieren. |
| **Testvoraussetzung** | Ein Lesezeichen für Benutzer und Text ist in der Datenbank. |
| **Testschritte** | 1. Ein zweites Lesezeichen für dieselbe Kombination speichern.<br>2. Anzahl Lesezeichen für Benutzer und Text abfragen. |
| **Erwartetes Ergebnis** | Die Datenbank lehnt das Speichern ab (Unique-Verletzung). Es gibt weiterhin genau 1 Lesezeichen. |
| **Tester / Datum** | |
| **Mängelklasse** | |
| **Mängelbeschreibung** | |

### T-024 – Daten kommen unverändert zurück

| | |
|---|---|
| **ID / Bezeichnung** | T-024 – Daten kommen unverändert zurück |
| **Beschreibung** | Gespeicherte Texte und Sitzungen werden ohne Veränderung wieder gelesen: Sonderzeichen, sehr lange Inhalte und Zeitstempel. |
| **Testvoraussetzung** | Ein Benutzer ist in der Datenbank. |
| **Testschritte** | 1. Text mit Umlauten und Emoji (`Grüezi Zürich – ß 📚`) und 100 000 Wörtern speichern.<br>2. Session mit festem Datum speichern.<br>3. Text und Session neu aus der Datenbank laden. |
| **Erwartetes Ergebnis** | Inhalt ist Zeichen für Zeichen gleich, Länge ist gleich, Datum ohne Verschiebung (Zeitzone). |
| **Tester / Datum** | |
| **Mängelklasse** | |
| **Mängelbeschreibung** | |

### T-025 – Ungültige Werte direkt in der DB

| | |
|---|---|
| **ID / Bezeichnung** | T-025 – Ungültige Werte direkt in der DB |
| **Beschreibung** | Werte ausserhalb der fachlichen Grenzen dürfen auch dann nicht gespeichert werden, wenn sie an der Anwendung vorbei geschrieben werden. |
| **Testvoraussetzung** | Ein Benutzer und ein Text sind in der Datenbank. |
| **Testschritte** | 1. Per SQL eine Session mit `comprehension_score = 150` und `wpm = 5` einfügen (an der Anwendung vorbei).<br>2. Anzahl Zeilen in `reading_sessions` abfragen. |
| **Erwartetes Ergebnis** | Die Datenbank lehnt das Einfügen ab. `reading_sessions` enthält 0 Zeilen. Grenzen: WPM 60–1500, Score 0–100. |
| **Tester / Datum** | |
| **Mängelklasse** | |
| **Mängelbeschreibung** | |
