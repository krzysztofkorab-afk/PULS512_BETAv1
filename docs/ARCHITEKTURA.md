# Architektura PULS 512

```mermaid
flowchart TD
    A[Wybrane kanały RSS] --> B[Parser na telefonie]
    B --> C[Ocena aktualności i źródła]
    C --> D[Usuwanie podobnych nagłówków]
    D --> E[Briefing 4–10 wiadomości]
    E --> F[Tekst i linki]
    E --> G[Odsłuch TTS]
    H[Godzina użytkownika] --> I[Alarm Android]
    I --> E
    J[Uwaga tekstowa lub głosowa] --> K[Aplikacja pocztowa]
    K --> L[lab512512@gmail.com]
```

W wydaniu produkcyjnym kroki B–D powinny zostać przeniesione na backend, a między D i E powinien działać model tworzący syntezę wyłącznie na podstawie przekazanych materiałów wraz z cytowaniem źródeł.
