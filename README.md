# BilimEDU API

Бэкенд электронного школьного журнала. Переписывание дипломного проекта
2023 года (ветка `main` этого репозитория, Node + MongoDB) на Java + Spring Boot +
PostgreSQL.

Полный план миграции и обоснование решений — в артефакте плана;
разбор долга исходной версии — в `ROADMAP.md`.

## Статус

**Фаза 02 — каркас и аутентификация.** Схема и контракт зафиксированы в фазе 01,
теперь на них лёг сервис: Spring Boot 4.1 на Java 21, Spring Security, модуль
`auth`. Предметные модули (классы, предметы, назначения, уроки, оценки) — впереди.

| | Артефакт |
|---|---|
| Схема БД | `src/main/resources/db/migration/` — 6 миграций Flyway |
| Проверка ограничений | `src/test/sql/schema_constraints.sql` — 12 проверок |
| Правила домена | `docs/domain-rules.md` |
| Матрица прав | `docs/permissions.md` |
| Контракт API | `docs/api-v1.md` |
| Приложение | `src/main/java/kz/bilimedu/api/` |
| Интеграционные тесты | 20 тестов против PostgreSQL в Testcontainers |

### Что уже работает

```
POST /api/v1/auth/login      POST /api/v1/auth/refresh    POST /api/v1/auth/logout
GET  /api/v1/me              POST /api/v1/me/password
GET  /api/v1/health          GET  /api/v1/docs
```

Публичны ровно четыре из них — `login`, `refresh`, `health`, `docs`;
всё остальное требует access-токена.

## Запуск

```sh
cp .env.example .env
openssl rand -base64 48                              # вписать в JWT_SECRET
docker compose up flyway --exit-code-from flyway     # поднять БД и накатить схему
./mvnw spring-boot:run
```

Без `JWT_SECRET` приложение не стартует — это осознанное поведение, а не
недосмотр: в версии 2023 года секрет был строкой `secret123` в исходниках.

Тесты поднимают свой PostgreSQL в контейнере, готовить базу для них не нужно:

```sh
./mvnw test
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

## Стек

| | |
|---|---|
| Java 21, Spring Boot 4.1 | Maven, wrapper в репозитории — ставить Maven отдельно не нужно |
| PostgreSQL 16 + Flyway | схема принадлежит миграциям, Hibernate её только валидирует |
| Spring Security 7 | свой JWT-фильтр: контракт требует различать `TOKEN_MISSING` и `TOKEN_EXPIRED` |
| Argon2id | хеш пароля; в версии 2023 года был bcrypt с секретом в коде |
| Testcontainers | тесты идут против настоящего PostgreSQL — половина правил живёт в ограничениях базы |

## Дальше

Модули по порядку: **users (CRUD от ADMIN)** → classes → subjects →
assignments → lessons → grades → notifications. Приёмочные критерии —
список тестов в конце `docs/permissions.md`: три из восьми закрыты.

Ближайшее препятствие — завести первого пользователя. Ручки создания
пользователей принадлежат `ADMIN`, а `ADMIN` пока неоткуда взяться:
нужен либо сид при старте, либо разовая миграция. Регистрации самозаписью
не будет — именно она была дырой версии 2023 года.
