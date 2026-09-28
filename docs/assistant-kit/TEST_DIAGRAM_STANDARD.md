# Renault Docs — Test Diagram Standard

Кожна route/test diagram починається з реальної точки входу користувача.

Показувати:
- parent screen;
- tap/action;
- child screen;
- dialog/system picker;
- важливі стани;
- Back/Cancel ownership;
- rotation/recreation point;
- expected PASS/FAIL.

Для lifecycle-sensitive flow явно показувати:

```text
before rotation
    ↓
Activity recreation
    ↓
same semantic state restored
    ↓
NO automatic operation restart
```

Якщо один child screen відкривається з різних parent — окремі routes, якщо Back destination різний.

Diagram визначає тест, але не є доказом виконання.
PASS/FAIL записується у test-run report після реального тесту.
