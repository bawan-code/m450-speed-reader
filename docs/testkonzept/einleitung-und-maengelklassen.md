# Testkonzept – Einleitung und Mängelklassifizierung (Person C)

Entwurf der Kapitel 1 und 4 für das Testkonzept LB2, nach `docs/PLAN-LB2.md` §6.3 (C4). Wird in das Word-Dokument übernommen.

---

## 1 Einleitung

Dieses Dokument beschreibt Planung, Durchführung und Ergebnisse der Integrationstests für den **Speed Reader**, die Software aus LB1.

Der Speed Reader zeigt einen Text Wort für Wort in einer wählbaren Geschwindigkeit (Wörter pro Minute, WPM) an. Jede Lesesitzung wird gespeichert; daraus berechnet die Anwendung die durchschnittliche und eine empfohlene Lesegeschwindigkeit. Zusätzlich merkt sie sich pro Benutzer und Text, bis zu welchem Wort gelesen wurde.

Das Backend ist eine Spring-Boot-Anwendung, die über Spring Data JPA auf eine MySQL-Datenbank mit vier Tabellen zugreift:

| Tabelle | Inhalt |
|---|---|
| `users` | Benutzer |
| `texts` | Texte, die gelesen werden |
| `reading_sessions` | Lesesitzungen: Geschwindigkeit, gelesene Wörter, Dauer, Datum, Verständnis-Score |
| `bookmarks` | Lesezeichen: aktuelle Position pro Benutzer und Text |

In LB1 wurde die Geschäftslogik mit Unit-Tests geprüft; die Datenbank war dabei durch Mocks ersetzt. **Ziel dieser Integrationstests** ist es, das Zusammenspiel zwischen dem Programmcode und einer echten Datenbank zu prüfen: Funktioniert die Verbindung, werden Daten vollständig und korrekt gespeichert, und schützt die Datenbank die Beziehungen zwischen den Daten?

Dazu wurden drei Schwerpunkte gewählt (Kapitel 3):

1. Datenbankverbindung
2. Transaktionen / CRUD
3. Datenintegrität

Pro Schwerpunkt werden fünf Testfälle beschrieben, ausgeführt und mit einer Mängelklasse bewertet (Kapitel 4 und 5). Die Testumgebung ist in Kapitel 2 beschrieben.

---

## 4 Mängelklassifizierung

Die festgestellten Mängel bzw. nicht erfüllten Anforderungen (Erwartungen) werden in Klassen von 1 bis 4 eingeteilt. Nur bei einem einwandfreien Ergebnis wird die Klasse 0 verwendet:

| Nr. | Mängelklasse | Beschreibung |
|---|---|---|
| 0 | mängelfrei | Einwandfrei und anforderungsgerecht |
| 1 | belangloser Mangel | Verwendung möglich, Brauchbarkeit ist vorhanden, Mängel sollte dennoch nicht vorkommen |
| 2 | leichter Mangel | Verwendung möglich, Brauchbarkeit ist nur wenig beeinträchtigt |
| 3 | schwerer Mangel | Verwendung ist noch möglich, Brauchbarkeit ist stark verringert |
| 4 | kritischer Mangel | Unbrauchbar; wesentliche Funktionalität ist nicht gegeben; Betrieb ist nicht verantwortbar (z.B. sicherheitsspezifisch) |

**Vorgehen bei einem Mangel:** Wird bei einem Testfall ein Mangel festgestellt, werden das tatsächliche Ergebnis, die Fehlermeldung und die Mängelklasse des ersten Testlaufs dokumentiert. Wird der Mangel behoben, wird der Test wiederholt und der Nachtest im selben Testfall festgehalten («Nachtest am …: bestanden, Mängelklasse 0»). Ein Mangel, der nicht behoben wird, bleibt mit seiner Klasse dokumentiert.
