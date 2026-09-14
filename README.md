# BilimEDU API

Бэкенд электронного школьного журнала. Переписывание дипломного проекта
2023 года (ветка `main` этого репозитория, Node + MongoDB) на Java + Spring Boot +
PostgreSQL.

Полный план миграции и обоснование решений — в артефакте плана;
разбор долга исходной версии — в `ROADMAP.md`.

## Статус

**Фаза 02 — каркас, аутентификация, справочники, зачисления.** Схема и
контракт зафиксированы в фазе 01, теперь на них лёг сервис: Spring Boot 4.1
на Java 21, Spring Security, модули `auth`, `users`, учебный календарь,
школьная структура и зачисления. Систему можно наполнить с нуля:
администратор заводится сидом, дальше — год, четверти, классы, предметы,
назначения учителей, зачисления учеников и переводы между классами.
Впереди журнал и оценки.

| | Артефакт |
|---|---|
| Схема БД | `src/main/resources/db/migration/` — 6 миграций Flyway |
| Проверка ограничений | `src/test/sql/schema_constraints.sql` — 12 проверок |
| Правила домена | `docs/domain-rules.md` |
| Матрица прав | `docs/permissions.md` |
| Контракт API | `docs/api-v1.md` |
| Приложение | `src/main/java/kz/bilimedu/api/` |
| Интеграционные тесты | 75 тестов против PostgreSQL в Testcontainers |

### Что уже работает

```
POST   /api/v1/auth/login    POST /api/v1/auth/refresh    POST /api/v1/auth/logout
GET    /api/v1/me            POST /api/v1/me/password
GET    /api/v1/health        GET  /api/v1/docs

GET    /api/v1/users?role=&classId=&includeDeactivated=&page=&size=
POST   /api/v1/users         GET  /api/v1/users/{id}
PATCH  /api/v1/users/{id}    DELETE /api/v1/users/{id}    → деактивация

GET    /api/v1/academic-years            POST /api/v1/academic-years
GET    /api/v1/terms?academicYearId=     POST /api/v1/terms
POST   /api/v1/terms/{id}/close          POST /api/v1/terms/{id}/reopen
GET    /api/v1/classes?academicYearId=   POST /api/v1/classes
GET    /api/v1/subjects?classId=         POST /api/v1/subjects
GET    /api/v1/teaching-assignments?teacherId=&classId=
POST   /api/v1/teaching-assignments      DELETE /api/v1/teaching-assignments/{id}

POST   /api/v1/enrollments               POST /api/v1/enrollments/{id}/transfer
GET    /api/v1/classes/{id}/roster?on=YYYY-MM-DD
GET    /api/v1/students/{id}/enrollments
```

Публичны ровно четыре — `login`, `refresh`, `health`, `docs`; всё остальное
требует access-токена. Писать справочники может только `ADMIN`. На чтении
работают предикаты владения: `TEACHER` видит коллег, учеников своих классов,
свои классы, свои назначения и состав своих классов; `STUDENT` — себя,
свой текущий класс, предметы этого класса и свою историю переводов,
а назначения и состав класса не видит вовсе.

## Запуск

```sh
cp .env.example .env
openssl rand -base64 48                              # вписать в JWT_SECRET
docker compose up flyway --exit-code-from flyway     # поднять БД и накатить схему
./mvnw spring-boot:run
```

**Первый запуск.** Ручки создания пользователей принадлежат `ADMIN`,
а регистрации самозаписью нет — именно она была дырой версии 2023 года.
Поэтому первый администратор заводится сидом: заполнить в `.env`

```
BOOTSTRAP_ADMIN_EMAIL=zavuch@school.kz
BOOTSTRAP_ADMIN_PASSWORD=<длинный пароль>
```

запустить приложение, затем убрать эти переменные и сменить пароль через
`POST /api/v1/me/password`. Сид срабатывает, только если активного
администратора в базе нет: повторный старт ничего не перезаписывает.

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

Нарушения ограничений базы переводятся в коды ошибок контракта по имени
ограничения (`ConstraintViolationTranslator`). Проверять «нет ли уже такой
записи» запросом перед вставкой бессмысленно: два параллельных запроса
пройдут проверку оба — именно так версия 2023 года плодила дубликаты.

## Дальше

Модули по порядку: ~~users~~ → ~~academic-years и terms~~ → ~~classes~~ →
~~subjects~~ → ~~teaching-assignments~~ → ~~enrollments~~ → **lessons** →
grades → notifications. Приёмочные критерии — список тестов в конце
`docs/permissions.md`: три из восьми закрыты, остальные четыре требуют
журнала и оценок.

Следующий шаг — уроки: `GET`/`POST /lessons`, планирование по назначению
и четверти. Там впервые заработают триггеры схемы — дата урока внутри
своей четверти (D3) и один СОЧ на четверть (D5), — и там же предикат P3
применится к урокам: переведённый ученик должен видеть сентябрьские уроки
прежнего класса и не видеть сентябрьские уроки нового.
