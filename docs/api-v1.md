# Контракт API v1

Все пути с префиксом `/api/v1`. В версии 2023 года версии не было вообще,
фильтры лежали в пути, а имена ресурсов были транслитом.

---

## Соглашения

**Именование.** Ресурсы — английские существительные во множественном числе.
Фильтры — в query, а не в сегментах пути. Действие выражается методом, а не
глаголом в URL: `DELETE /teachers/1`, а не `DELETE /delete/teacher/1`.

**Формат ошибки.** Один на все случаи. В версии 2023 года их было три:
`{message}`, массив от `express-validator` и голая строка.

```json
{
  "error": {
    "code": "GRADE_EXCEEDS_MAX",
    "message": "Балл 99 превышает максимум 15 за этот урок",
    "details": [{ "field": "score", "max": 15 }]
  }
}
```

`code` — стабильный машинный идентификатор, по нему клиенты ветвятся.
`message` — уже локализованный текст по `Accept-Language` (`ru`, `kk`).

**Пагинация.** На всех коллекциях, без исключений: `?page=0&size=50`.
В версии 2023 года `GET /marksall` отдавал все оценки школы одним массивом.

```json
{ "items": [], "page": 0, "size": 50, "total": 0 }
```

**Идентификаторы.** `bigint`, в JSON — строки, чтобы не терять точность в JS.

---

## Соответствие старым маршрутам

| Было | Стало |
|---|---|
| `POST /auth/login` · `POST /auth/loginStudent` | `POST /auth/login` — одна ручка на все роли |
| `GET /auth/me` · `GET /auth/student/me` | `GET /me` |
| `POST /auth/change-password/teacher/:id` | `POST /me/password` — id берётся из токена |
| `GET /teacher` · `GET /students` | `GET /users?role=TEACHER` |
| `PUT /teacher/:id` (добавить permission) | `POST /teaching-assignments` |
| `DELETE /teacher/:id/:permissionId` | `DELETE /teaching-assignments/{id}` |
| `PUT /student/changeClass/:id` | `POST /enrollments/{id}/transfer` |
| `GET /predmet` · `POST /subject` | `GET` · `POST /subjects` |
| `GET /predmet/class/:id` | `GET /subjects?classId=` |
| `POST /ktp` · `GET /ktps` | `POST` · `GET /lessons` |
| `GET /ktp/period/:classId/:period/:predmetId` | `GET /lessons?assignmentId=&termId=` |
| `POST /marks` | `POST /lessons/{id}/grades` |
| `GET /marks/final/:studentId/:predmetId/:type/:period` | `GET /students/{id}/term-grades?termId=` |
| `DELETE /marks/:studentId/:ktpId` | `DELETE /grades/{id}` |
| `POST /marks/:chat_id/:message` | — удалён, уведомления уходят через outbox |
| `POST /telegramStudent/:username/:studentId` | `POST /me/telegram/link` |

Две ручки исчезают осознанно. `POST /marks/:chat_id/:message` передавал текст
уведомления в сегменте URL и дублировал `POST /marks`; отправка теперь —
следствие выставления оценки, а не отдельный вызов клиента. `POST /telegram*`
принимал чужой `studentId` в пути — теперь пользователь привязывает только
свой чат, id берётся из токена.

---

## Ресурсы

### Аутентификация

```
POST   /auth/login          { email, password } → { accessToken, refreshToken, user }
POST   /auth/refresh        { refreshToken }    → { accessToken, refreshToken }
POST   /auth/logout         { refreshToken }    → 204
GET    /me                                      → User
POST   /me/password         { current, new }    → 204   (отзывает все refresh-токены)
POST   /me/telegram/link    { code }            → 204
```

### Справочники · ADMIN

```
GET    /users?role=&classId=&includeDeactivated=&page=&size=
POST   /users               { fullName, email, role, password }     → 201
GET    /users/{id}
PATCH  /users/{id}          { fullName, email, avatarUrl }
DELETE /users/{id}                              → деактивация, не удаление

GET    /academic-years          POST /academic-years
GET    /terms?academicYearId=   POST /terms
POST   /terms/{id}/close                        → заморозка итоговых
POST   /terms/{id}/reopen

GET    /classes?academicYearId= POST /classes
GET    /subjects                POST /subjects
GET    /teaching-assignments?teacherId=&classId=
POST   /teaching-assignments    DELETE /teaching-assignments/{id}
```

Чтение `users` доступно всем ролям, но показывает разное (`permissions.md`):
`ADMIN` — всех, `TEACHER` — коллег-учителей и учеников своих классов,
`STUDENT` — только себя. Видимость встроена в запрос, а не отфильтрована
поверх результата: иначе `total` выдавал бы существование скрытых записей.
Чужая запись отвечает `403 NOT_OWNER`.

`PATCH /users/{id}` не меняет роль. Учитель связан с `teaching_assignments`
и остаётся автором оценок (`grades.graded_by` с `ON DELETE RESTRICT`), поэтому
превращение его в ученика оставило бы назначения без учителя. Смена роли —
это деактивация одной записи и заведение другой.

`DELETE /users/{id}` немедленно отзывает все refresh-токены пользователя:
иначе уволенный работал бы по живому токену ещё тридцать дней.
Деактивированные не попадают в списки без `?includeDeactivated=true`.

`POST /terms/{id}/close` сейчас выставляет только признак `closed_at`,
который запрещает правку оценок (D4). Заморозка итоговых в `term_grades`
(D14) появится вместе с модулем `grades`: до него замораживать нечего,
а веса СОР и СОЧ берутся из нормативного акта, а не из кода (D12).
`reopen` идемпотентен — закрытие четверти обратимое решение завуча.

`DELETE /teaching-assignments/{id}` отказывает, если у назначения есть
уроки: `lessons.assignment_id` объявлен с `ON DELETE CASCADE`, и удаление
унесло бы журнал вместе с оценками.

Назначить на предмет можно только активного пользователя с ролью
`TEACHER`. Ограничением базы это не выражается — внешний ключ ведёт на
`users`, где лежат все три роли, и «ученик ведёт математику» прошёл бы.

### Зачисления

```
GET    /classes/{id}/roster?on=YYYY-MM-DD        → состав класса на дату
POST   /enrollments             { studentId, classId, fromDate }
POST   /enrollments/{id}/transfer { toClassId, fromDate }
GET    /students/{id}/enrollments                → история переводов
```

`?on=` по умолчанию — сегодня. Именно здесь окупается временна́я модель:
состав класса на любую прошлую дату получается без отдельной таблицы истории.

`POST /enrollments/{id}/transfer` не меняет класс у ученика, а закрывает
действующее зачисление днём до перехода и открывает новое. Граница
включительна: при переводе с 12 января последний день в прежнем классе —
11 января, и состав прежнего класса на 11-е ещё содержит ученика.

Состав класса ученику не показывается вовсе: по матрице прав он читает
свои зачисления, а не список одноклассников. `GET /classes/{id}/roster`
доступен `ADMIN` и `TEACHER`, причём учитель — только по своим классам
(иначе `403 NOT_OWNER`). `GET /students/{id}/enrollments` ученик читает
только про себя, учитель — только про учеников своих классов.

Зачислить можно только активного пользователя с ролью `STUDENT`:
внешний ключ ведёт на `users`, где лежат все три роли, и «учитель зачислен
в 8А» база пропустила бы.

### Планирование и журнал · TEACHER

```
GET    /lessons?assignmentId=&termId=&page=&size=
POST   /lessons             { assignmentId, termId, title, lessonDate, kind, maxScore }
PATCH  /lessons/{id}
DELETE /lessons/{id}                             → каскадом удаляет оценки урока

GET    /lessons/{id}/grades                      → журнал по уроку
PUT    /lessons/{id}/grades  [{ studentId, score }]   → bulk, идемпотентно
PATCH  /grades/{id}         { score, reason }
DELETE /grades/{id}         { reason }

PUT    /lessons/{id}/attendance [{ studentId, status }]
GET    /grades/{id}/audit
```

`PUT /lessons/{id}/grades` массивом — замена поштучного `POST /marks`:
учитель выставляет оценки всему классу одним действием, повторная отправка
того же тела ничего не меняет.

**Уроки.** Писать их может только `TEACHER`, и только в своих назначениях
(предикат P1) — `ADMIN` журнал читает, но не ведёт, по той же причине, по
которой не правит оценки. Ученик видит урок, если числился в классе
**на дату урока** (предикат P3), а не по текущему классу.

`PATCH /lessons/{id}` меняет только название, дату и максимальный балл.
Назначение, четверть и тип оценивания неизменны: перенос урока в другое
назначение оставил бы выставленные оценки учеников чужого класса, а смена
`SOR` на `SOCH` обошла бы правило D5 для уже проведённой работы.

Четверть урока должна принадлежать тому же учебному году, что и класс
назначения. Схема этого не выражает — `lessons` ссылается на `terms` и на
`teaching_assignments` независимо, а триггер сверяет только даты, — поэтому
проверка лежит в сервисе и отвечает `TERM_YEAR_MISMATCH`.

Создание урока в закрытой четверти не запрещено: правило D4 говорит об
оценках, а в закрытой четверти их всё равно не выставить. Если это нужно
запрещать, правило стоит сначала внести в `domain-rules.md`.

### Успеваемость · STUDENT

```
GET    /students/{id}/grades?termId=&subjectId=
GET    /students/{id}/term-grades?termId=
GET    /students/{id}/attendance?termId=
```

`{id}` обязан совпадать с `sub` токена для роли `STUDENT` — иначе 403.
Путь оставлен явным, а не заменён на `/me/grades`, чтобы теми же ручками
пользовался учитель, глядя на своего ученика.

---

## Коды ошибок

| HTTP | `code` | Когда |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean Validation на DTO |
| 401 | `TOKEN_MISSING` · `TOKEN_EXPIRED` | Нет или истёк access-токен |
| 401 | `TOKEN_INVALID` | Подпись не сходится или токен повреждён |
| 401 | `INVALID_CREDENTIALS` | Неверная пара email + пароль |
| 401 | `REFRESH_TOKEN_INVALID` | Refresh-токен неизвестен, отозван или истёк |
| 403 | `ROLE_FORBIDDEN` · `NOT_OWNER` | Роль не та; ресурс чужой |
| 403 | `ACCOUNT_DEACTIVATED` | `users.deactivated_at` не NULL |
| 404 | `NOT_FOUND` | |
| 409 | `EMAIL_ALREADY_EXISTS` | Email занят другим пользователем |
| 409 | `CANNOT_DEACTIVATE_SELF` | Администратор деактивирует сам себя |
| 409 | `ACADEMIC_YEAR_ALREADY_EXISTS` | Год с таким названием уже заведён |
| 409 | `TERM_OVERLAPS` | Правило D2 |
| 409 | `TERM_ORDINAL_TAKEN` | Правило D1 |
| 409 | `CLASS_ALREADY_EXISTS` | Класс с таким именем в этом году уже есть |
| 409 | `SUBJECT_ALREADY_EXISTS` | Предмет с таким названием уже есть |
| 409 | `ASSIGNMENT_ALREADY_EXISTS` | У пары «класс + предмет» уже есть учитель |
| 409 | `ASSIGNMENT_HAS_LESSONS` | Удаление назначения унесло бы журнал |
| 409 | `LESSON_ALREADY_EXISTS` | Дубль урока в назначении на ту же дату |
| 409 | `ENROLLMENT_NOT_ACTIVE` | Перевод оформляют по действующему зачислению |
| 409 | `ALREADY_IN_CLASS` | Перевод в класс, где ученик и так числится |
| 422 | `NOT_A_TEACHER` | На предмет назначают только активного `TEACHER` |
| 422 | `NOT_A_STUDENT` | В класс зачисляют только активного `STUDENT` |
| 422 | `TERM_YEAR_MISMATCH` | Четверть из другого учебного года, чем класс урока |
| 500 | `INTERNAL_ERROR` | Непредвиденная ошибка; стектрейс не уходит клиенту |
| 409 | `SOCH_ALREADY_EXISTS` | Правило D5 |
| 409 | `GRADE_ALREADY_EXISTS` | Правило D6 |
| 409 | `ENROLLMENT_OVERLAPS` | Правило временно́й модели |
| 409 | `TERM_CLOSED` | Правило D4 |
| 422 | `GRADE_EXCEEDS_MAX` | Правило D7 |
| 422 | `STUDENT_NOT_ENROLLED` | Правило D9 |
| 422 | `LESSON_OUTSIDE_TERM` | Правило D3 |

Каждый код 409 и 422 соответствует ограничению из `domain-rules.md`.
Сервисный слой ловит нарушение ограничения базы и переводит его в код —
источником истины остаётся база, а не проверка в приложении.

Нарушения ограничений переводятся в коды по имени ограничения
PostgreSQL — `ConstraintViolationTranslator`. Имена взяты запросом
к `pg_constraint` на накатанных миграциях, а не угаданы; правила
триггеров (`LESSON_OUTSIDE_TERM`, `GRADE_EXCEEDS_MAX`) опознаются по
тексту, который формирует сам триггер, — у них нет имени ограничения.

Коды добавлены в фазе 02: первая редакция контракта не описывала ни
случаи аутентификации, ни конфликты справочников. `INVALID_CREDENTIALS` намеренно один на «нет такого
email» и «неверный пароль» — иначе ответ выдаёт, какие адреса заведены
в системе.

**Смена пароля.** Поле тела `POST /me/password` называется `new`; это
ключевое слово Java, поэтому в коде компонент записи назван иначе, а имя
в JSON задано явно. Контракт не меняется.
