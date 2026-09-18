# BilimEDU API

Бэкенд электронного школьного журнала. Переписывание дипломного проекта
2023 года (ветка `main` этого репозитория, Node + MongoDB) на Java + Spring Boot +
PostgreSQL.

Полный план миграции и обоснование решений — в артефакте плана;
разбор долга исходной версии — в `ROADMAP.md`.

## Статус

**Фаза 02 завершена в основной части.** Схема и контракт зафиксированы
в фазе 01, на них лёг сервис: Spring Boot 4.1 на Java 21, Spring Security,
модули `auth`, `users`, учебный календарь, школьная структура, зачисления,
уроки, оценки с аудитом и посещаемость. Школу можно провести через полный
цикл: завести год и четверти, классы, предметы и назначения, зачислить
учеников, запланировать уроки, выставить оценки с историей правок
и отметить посещаемость.

**Все восемь приёмочных критериев из `docs/permissions.md` закрыты.**
Осталось: уведомления в Telegram и расчёт итоговых за четверть — второй
заблокирован отсутствием нормативных коэффициентов (см. «Дальше»).

| | Артефакт |
|---|---|
| Схема БД | `src/main/resources/db/migration/` — 6 миграций Flyway |
| Проверка ограничений | `src/test/sql/schema_constraints.sql` — 12 проверок |
| Правила домена | `docs/domain-rules.md` |
| Матрица прав | `docs/permissions.md` |
| Контракт API | `docs/api-v1.md` |
| Приложение | `src/main/java/kz/bilimedu/api/` |
| Интеграционные тесты | 124 теста против PostgreSQL в Testcontainers |

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

GET    /api/v1/lessons?assignmentId=&termId=   GET /api/v1/lessons/{id}
POST   /api/v1/lessons                   PATCH /api/v1/lessons/{id}
DELETE /api/v1/lessons/{id}              → каскадом уносит оценки урока

GET    /api/v1/lessons/{id}/grades       журнал по уроку
PUT    /api/v1/lessons/{id}/grades       списком, идемпотентно
PATCH  /api/v1/grades/{id}               { score, reason }
DELETE /api/v1/grades/{id}               { reason }
GET    /api/v1/grades/{id}/audit         история правок, переживает удаление
GET    /api/v1/students/{id}/grades?termId=&subjectId=

GET    /api/v1/lessons/{id}/attendance   ведомость по уроку
PUT    /api/v1/lessons/{id}/attendance   списком, четыре статуса
GET    /api/v1/students/{id}/attendance?termId=
```

Публичны ровно четыре — `login`, `refresh`, `health`, `docs`; всё остальное
требует access-токена. Писать справочники может только `ADMIN`. На чтении
работают предикаты владения: `TEACHER` видит коллег, учеников своих классов,
свои классы, свои назначения, состав своих классов и уроки своих
назначений; `STUDENT` — себя, свой текущий класс, предметы этого класса,
свою историю переводов, свои оценки и уроки того класса, в котором
числился **на дату урока**. Уроки, оценки и посещаемость пишет только
`TEACHER` и только в своих назначениях: завуч журнал читает, но не ведёт
и не правит — иначе аудит правок потерял бы смысл.

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
~~subjects~~ → ~~teaching-assignments~~ → ~~enrollments~~ → ~~lessons~~ →
~~grades~~ → ~~attendance~~ → **notifications**. Приёмочные критерии
в конце `docs/permissions.md` — все восемь закрыты.

**Уведомления** — `POST /me/telegram/link` и воркер, вычитывающий `outbox`.
В версии 2023 года это был `axios.post` без `await` прямо в контроллере:
ошибка отправки терялась молча, а время ответа Telegram попадало во время
ответа API.

**Итоговые за четверть (D12–D15) заблокированы.** Веса СОР и СОЧ и пороги
перевода в пятибалльную шкалу задаются нормативным актом МОН РК и со
временем менялись. Их нужно взять из действующего приказа и внести
в конфигурацию, а не в код. Структура расчёта в `domain-rules.md` уже
зафиксирована, место для заморозки (`term_grades.formula_version`) в схеме
есть — не хватает только чисел.
