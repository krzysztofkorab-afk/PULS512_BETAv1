# PULS 512 — beta v0.2

**Twój świat. Dwa razy dziennie.**

PULS 512 to aplikacja Android przygotowująca krótki, weryfikowany briefing z wybranych źródeł informacyjnych. Użytkownik sam wybiera zakres wiadomości, liczbę pozycji oraz godziny porannego i wieczornego wydania.

## Co działa w wersji beta

- zakresy: Polska, Europa, Świat, Biznes i Technologia;
- wybór 4–10 informacji w jednym briefingu;
- dwa niezależne raporty z własną godziną i możliwością wyłączenia;
- pobieranie 17 kanałów RSS/Atom od instytucji i wydawców;
- grupowanie publikacji dotyczących tego samego wydarzenia;
- trzy statusy: Potwierdzona, Źródło oficjalne i Rozwijająca się;
- domyślny tryb „Tylko potwierdzone”;
- potwierdzenie przez oficjalne dane albo co najmniej dwa niezależne źródła;
- jawny powód oceny, liczba źródeł i sekcja „Dlaczego to ważne”;
- otwieranie oryginalnego artykułu;
- czytanie briefingu polskim głosem systemowym Androida;
- powiadomienia oraz ponowne planowanie po restarcie telefonu;
- uwagi tekstowe kierowane na `lab512512@gmail.com`;
- nagrywanie uwagi głosowej i dodawanie pliku do wiadomości e-mail.

## Silnik anty-dezinformacyjny

- oficjalne dane pierwotne mogą otrzymać status `CONFIRMED` bez drugiej publikacji;
- oficjalne oświadczenie dotyczące faktów zewnętrznych otrzymuje status `OFFICIAL_SOURCE`, dopóki nie pojawi się niezależne potwierdzenie;
- pojedynczy materiał medialny otrzymuje status `DEVELOPING` i jest ukryty w trybie ścisłym;
- model AI nie wybiera poziomu wiarygodności i nie może tworzyć linków — robi to deterministyczny kod;
- podsumowanie AI może używać wyłącznie przekazanych materiałów źródłowych.

## Backend

W katalogu `backend` znajduje się gotowy serwer Node.js 20. Bez klucza OpenAI nadal grupuje i ocenia źródła. Po ustawieniu `OPENAI_API_KEY` tworzy krótkie polskie podsumowania przez Responses API, wymuszając wynik zgodny ze schematem JSON.

Szczegóły: `backend/README.md`.

## Ważne ograniczenia bety

1. RSS nie gwarantuje pełnego obrazu wydarzeń. Wydawca może zmienić lub wyłączyć kanał.
2. Bez wdrożonego backendu streszczenie jest ekstrakcyjne — aplikacja oczyszcza i skraca opis dostarczony w kanale.
3. Klucza API AI nie wolno umieszczać w aplikacji ani w repozytorium. Dołączony backend odczytuje go wyłącznie ze zmiennej środowiskowej.
4. Na Androidzie 12+ dokładność alarmu zależy od przyznanego uprawnienia do dokładnych alarmów i ustawień oszczędzania baterii. Bez niego system może dostarczyć raport kilka–kilkanaście minut później.
5. Wysyłka opinii otwiera klienta pocztowego użytkownika. Użytkownik zatwierdza wysłanie wiadomości i nagrania.

## Szybki start na GitHub

1. Utwórz puste repozytorium, np. `puls-512`.
2. Wgraj **zawartość** tego katalogu do głównego katalogu repozytorium.
3. Opcjonalnie wdroż katalog `backend` i dodaj adres jako zmienną repozytorium `PULS512_API_BASE_URL`.
4. Otwórz kartę **Actions** i uruchom workflow `Build PULS 512 beta` albo wykonaj dowolny push do gałęzi `main`.
5. Po zakończeniu pobierz artefakt `PULS512-beta-apk`.
6. Rozpakuj go i zainstaluj `app-debug.apk` na telefonie. Android może poprosić o zgodę na instalację z danego źródła.

## Budowanie lokalne

Wymagane są Android SDK 35, Java 17 i Gradle 8.10.2.

```bash
gradle :app:assembleDebug
```

APK powstanie w `app/build/outputs/apk/debug/app-debug.apk`.

## Gdzie zmieniać najważniejsze elementy

| Element | Plik |
|---|---|
| Nazwa, kolory i ekran aplikacji | `app/src/main/java/pl/lab512/puls512/MainActivity.kt` |
| Paleta graficzna | `app/src/main/java/pl/lab512/puls512/ui/PulsTheme.kt` |
| Źródła i lokalne reguły weryfikacji | `app/src/main/java/pl/lab512/puls512/data/NewsRepository.kt` |
| Backend, AI i centralny katalog źródeł | `backend/server.mjs` |
| Harmonogram | `app/src/main/java/pl/lab512/puls512/schedule/AlarmScheduler.kt` |
| Adres e-mail uwag | `MainActivity.kt`, funkcje `sendTextFeedback` i `sendVoiceFeedback` |

## Przed wydaniem produkcyjnym

Backend powinien co najmniej:

- zawrzeć umowy/licencje na płatne serwisy agencyjne;
- dodać monitoring niedziałających kanałów;
- prowadzić jawny rejestr sprostowań;
- odróżniać informacje od opinii i komentarzy;
- przechowywać klucz do modelu AI wyłącznie jako sekret serwera;
- pozwalać na centralne wyłączenie niedziałającego kanału bez aktualizacji aplikacji.

## Identyfikacja wizualna

- tło: `#071018`;
- powierzchnie: `#0D1923`, `#132431`;
- akcent lodowy: `#73DBFF`;
- potwierdzenie: `#69E1BA`;
- ostrzeżenie/nagrywanie: `#FF8D78`;
- font: systemowy sans, duże nagłówki w odmianie Black/Bold.

Pakiet: `pl.lab512.puls512`  
Wersja: `0.2.0-beta`
