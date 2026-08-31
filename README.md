# BilimEDU API

Бэкенд электронного школьного журнала. Переписывание дипломного проекта
2023 года (ветка `main` этого репозитория, Node + MongoDB) на Java + Spring Boot +
PostgreSQL.

Полный план миграции и обоснование решений — в артефакте плана;
разбор долга исходной версии — в `ROADMAP.md`.

## Статус

**Фаза 01 — контракт и схема.** Кода приложения ещё нет: сначала фиксируются
модель данных и контракт, потом пишется сервис.

| | Артефакт |
|---|---|
| Схема БД | `src/main/resources/db/migration/` — 6 миграций Flyway |
| Проверка ограничений | `src/test/sql/schema_constraints.sql` — 12 проверок |
| Правила домена | `docs/domain-rules.md` |
| Матрица прав | `docs/permissions.md` |
| Контракт API | `docs/api-v1.md` |

## Запуск

```sh
cp .env.example .env
docker compose up flyway --exit-code-from flyway     # поднять БД и накатить схему
```

Проверить, что доменные правила действительно живут в базе:

```sh
docker compose exec -T db psql -U bilimedu -d bilimedu \
    -v ON_ERROR_STOP=1 -f - < src/test/sql/schema_constraints.sql
```

Ожидается 12 строк `PASS`. Скрипт откатывает транзакцию — база остаётся чистой.

## Что проверяет схема

Ограничения стоят в базе намеренно: проверка в коде приложения обходится
гонкой двух параллельных запросов, чем и страдала версия 2023 года.

| Правило | Механизм |
|---|---|
| Один СОЧ в четверти по предмету у класса | частичный `UNIQUE INDEX` |
| Одна оценка на ученика за урок | `UNIQUE (lesson_id, student_id)` |
| Балл не выше максимума урока | триггер |
| Дата урока внутри своей четверти | триггер |
| Четверти года не пересекаются | `EXCLUDE USING gist` |
| Ученик не в двух классах одновременно | `EXCLUDE USING gist` |

## Дальше

Фаза 02 — каркас Spring Boot, Spring Security и модули по порядку:
auth → classes → subjects → assignments → lessons → grades → notifications.
Приёмочные критерии — список тестов в конце `docs/permissions.md`.
