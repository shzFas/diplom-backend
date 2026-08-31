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
GET    /users?role=&classId=&page=&size=
POST   /users               { fullName, email, role, password }
GET    /users/{id}
PATCH  /users/{id}
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

### Зачисления

```
GET    /classes/{id}/roster?on=YYYY-MM-DD        → состав класса на дату
POST   /enrollments             { studentId, classId, fromDate }
POST   /enrollments/{id}/transfer { toClassId, fromDate }
GET    /students/{id}/enrollments                → история переводов
```

`?on=` по умолчанию — сегодня. Именно здесь окупается временна́я модель:
состав класса на любую прошлую дату получается без отдельной таблицы истории.

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

Шесть кодов добавлены в фазе 02: первая редакция контракта не описывала
случаи аутентификации. `INVALID_CREDENTIALS` намеренно один на «нет такого
email» и «неверный пароль» — иначе ответ выдаёт, какие адреса заведены
в системе.

**Смена пароля.** Поле тела `POST /me/password` называется `new`; это
ключевое слово Java, поэтому в коде компонент записи назван иначе, а имя
в JSON задано явно. Контракт не меняется.
