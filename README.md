# PULS 512 — beta

**Twój świat. Dwa razy dziennie.**

PULS 512 to aplikacja Android przygotowująca krótki briefing z wybranych źródeł informacyjnych. Użytkownik sam wybiera zakres wiadomości, liczbę pozycji oraz godziny porannego i wieczornego wydania.

## Co działa w wersji beta

- zakresy: Polska, Europa, Świat, Biznes i Technologia;
- wybór 4–10 informacji w jednym briefingu;
- dwa niezależne raporty z własną godziną i możliwością wyłączenia;
- pobieranie kanałów RSS/Atom bezpośrednio od wybranych wydawców;
- premiowanie aktualności i źródeł o wyższej wadze redakcyjnej;
- usuwanie bardzo podobnych nagłówków;
- otwieranie oryginalnego artykułu;
- czytanie briefingu polskim głosem systemowym Androida;
- powiadomienia oraz ponowne planowanie po restarcie telefonu;
- uwagi tekstowe kierowane na `lab512512@gmail.com`;
- nagrywanie uwagi głosowej i dodawanie pliku do wiadomości e-mail.

## Ważne ograniczenia bety

1. RSS nie gwarantuje pełnego obrazu wydarzeń. Wydawca może zmienić lub wyłączyć kanał.
2. Obecne streszczenie jest ekstrakcyjne — aplikacja oczyszcza i skraca opis dostarczony w kanale. Pełne porównywanie kilku publikacji i synteza AI wymagają bezpiecznego serwera pośredniego.
3. Klucza API AI nie wolno umieszczać w aplikacji ani w repozytorium. W wersji produkcyjnej aplikacja powinna komunikować się z własnym backendem.
4. Na Androidzie 12+ dokładność alarmu zależy od przyznanego uprawnienia do dokładnych alarmów i ustawień oszczędzania baterii. Bez niego system może dostarczyć raport kilka–kilkanaście minut później.
5. Wysyłka opinii otwiera klienta pocztowego użytkownika. Użytkownik zatwierdza wysłanie wiadomości i nagrania.

## Szybki start na GitHub

1. Utwórz puste repozytorium, np. `puls-512`.
2. Wgraj **zawartość** tego katalogu do głównego katalogu repozytorium.
3. Otwórz kartę **Actions** i uruchom workflow `Build PULS 512 beta` albo wykonaj dowolny push do gałęzi `main`.
4. Po zakończeniu pobierz artefakt `PULS512-beta-apk`.
5. Rozpakuj go i zainstaluj `app-debug.apk` na telefonie. Android może poprosić o zgodę na instalację z danego źródła.

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
| Źródła RSS i ich wagi | `app/src/main/java/pl/lab512/puls512/data/NewsRepository.kt` |
| Harmonogram | `app/src/main/java/pl/lab512/puls512/schedule/AlarmScheduler.kt` |
| Adres e-mail uwag | `MainActivity.kt`, funkcje `sendTextFeedback` i `sendVoiceFeedback` |

## Następny etap produkcyjny

Backend powinien co najmniej:

- pobierać źródła i trzymać cache;
- klastrować to samo wydarzenie opisane przez różne redakcje;
- generować krótkie podsumowanie po polsku z przypisaniem źródeł;
- oznaczać wiadomości potwierdzone przez co najmniej dwa niezależne źródła;
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
Wersja: `0.1.0-beta`
