# Backend weryfikacyjny PULS 512

Backend pobiera kanały źródłowe, grupuje publikacje dotyczące tego samego wydarzenia i nadaje im status wiarygodności. Dopiero potem opcjonalnie przekazuje zaakceptowane materiały do modelu AI, aby utworzyć krótkie polskie podsumowanie.

## Zasady publikacji

- `CONFIRMED`: oficjalne dane pierwotne albo zgodność co najmniej dwóch niezależnych wydawców;
- `OFFICIAL_SOURCE`: komunikat instytucji, jeszcze bez niezależnego potwierdzenia;
- `DEVELOPING`: pojedyncze źródło; domyślnie nie trafia do briefingu.

Model AI nie wybiera źródeł, nie zmienia linków i nie ustala poziomu wiarygodności. Dostaje wyłącznie teksty z klastrów zaakceptowanych wcześniej przez kod.

## Uruchomienie

Wymagany jest Node.js 20 lub nowszy.

```bash
cp .env.example .env
set -a
. ./.env
set +a
npm test
npm start
```

Serwer udostępnia:

- `GET /health` – stan backendu;
- `GET /sources` – aktywny katalog źródeł;
- `GET /briefing?categories=POLSKA,EUROPA,SWIAT&limit=6&verifiedOnly=true` – briefing.

## OpenAI

`OPENAI_API_KEY` jest opcjonalny. Bez niego backend nadal grupuje i weryfikuje źródła, ale używa skrótu pobranego z RSS. Z kluczem korzysta z Responses API i wymusza odpowiedź zgodną ze schematem JSON.

Klucz ustawiaj wyłącznie jako sekret środowiska serwera. Nie wpisuj go do aplikacji, pliku Gradle ani repozytorium.

## Połączenie aplikacji z backendem

Podczas budowania Androida przekaż adres:

```bash
gradle :app:assembleDebug -PPULS512_API_BASE_URL=https://twoj-backend.example.com
```

W GitHub Actions najlepiej zapisać pełny adres jako zmienną repozytorium `PULS512_API_BASE_URL` i przekazać ją w poleceniu budowania. Backend można uruchomić na dowolnej usłudze obsługującej Node.js 20, np. jako Web Service.

Przed komercyjnym uruchomieniem trzeba sprawdzić warunki wykorzystania każdego kanału. Pełne serwisy agencyjne Reuters, AP, AFP i PAP mogą wymagać osobnej umowy lub licencji; nie należy ich kopiować ani scrapować bez uprawnienia.
