# Architektura PULS 512

```mermaid
flowchart TD
    A[Źródła oficjalne i redakcje] --> B[Backend: parser i cache]
    B --> C[Grupowanie tego samego wydarzenia]
    C --> D[Reguły potwierdzenia]
    D --> E[AI: polski skrót ze schematem JSON]
    E --> F[Briefing 4–10 wiadomości]
    F --> G[Tekst, linki i TTS]
    H[Godzina użytkownika] --> I[Alarm Android]
    I --> F
    J[Uwaga tekstowa lub głosowa] --> K[Aplikacja pocztowa]
    K --> L[lab512512@gmail.com]
```

Gdy backend nie jest skonfigurowany, aplikacja uruchamia lokalny wariant kroków B–D i pokazuje skróty RSS bez generowania AI. Klucz API istnieje wyłącznie po stronie serwera.
