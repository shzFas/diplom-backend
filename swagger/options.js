export const options = {
  definition: {
    openapi: "3.0.3",
    info: {
      title: "Diplom Server",
      contact: {
        url: "https://github.com/shzfas",
      },
      version: "1.0",
    },
    servers: [
      {
        url: "https://diplom-backend.onrender.com",
      },
    ],
    security: [{ bearerAuth: [] }],
    tags: [
      {
        name: "Authorize",
        description: "Sign in, Sign Up",
      },
      {
        name: "Change password users",
        description: "Before work needs Bearer token",
      },
      {
        name: "Teachers",
        description: "CRUD teachers",
      },
      {
        name: "Students",
        description: "CRUD students",
      },
      {
        name: "Classes",
        description: "CRUD classes",
      },
      {
        name: "Subjects",
        description: "CRUD subjects",
      },
      {
        name: "KTP",
        description: "CRUD KTP",
      },
      {
        name: "Marks",
        description: "CRUD marks",
      },
    ],
    paths: {
      "/auth/login": {
        post: {
          tags: ["Authorize"],
          summary: "Login as Teacher/Admin",
          requestBody: {
            description: "Sign in",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/UserLogin",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/UserLogin",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/UserLogin",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Return JWT token",
            },
            400: {
              description: "Email and Password invalid",
            },
            404: {
              description: "User not found",
            },
          },
        },
      },
      "/auth/register": {
        post: {
          tags: ["Authorize"],
          summary: "Sign up as Teacher/Admin",
          requestBody: {
            description: "Sign up",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/UserRegister",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/UserRegister",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/UserRegister",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Return User Info",
            },
            400: {
              description: "Validation error",
            },
            500: {
              description: "Error register",
            },
          },
        },
      },
      "/auth/loginStudent": {
        post: {
          tags: ["Authorize"],
          summary: "Login as Student",
          requestBody: {
            description: "Sign in",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/StudentLogin",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/StudentLogin",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/StudentLogin",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Return JWT token",
            },
            400: {
              description: "Email and Password invalid",
            },
            404: {
              description: "User not found",
            },
          },
        },
      },
      "/auth/registerStudent": {
        post: {
          tags: ["Authorize"],
          summary: "Sign up as Student",
          requestBody: {
            description: "Sign up",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/StudentRegister",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/StudentRegister",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/StudentRegister",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Return User Info",
            },
            400: {
              description: "Validation error",
            },
            500: {
              description: "Error register",
            },
          },
        },
      },
      "/auth/me": {
        get: {
          tags: ["Authorize"],
          summary: "Sign in as Teacher/Admin",
          description: "Need paste JWT Token in Authorize",
          security: [{ bearerAuth: [] }],
          responses: {
            200: {
              description: "Return User",
            },
            403: {
              description: "Not access",
            },
          },
        },
      },
      "/auth/student/me": {
        get: {
          tags: ["Authorize"],
          summary: "Sign in as Student",
          description: "Need paste JWT Token in Authorize",
          security: [{ bearerAuth: [] }],
          responses: {
            200: {
              description: "Return User",
            },
            403: {
              description: "Not access",
            },
          },
        },
      },
      "/auth/change-password/teacher/{_id}": {
        post: {
          tags: ["Change password users"],
          summary: "Change password Admin/Teacher",
          description: "Need paste JWT Token in Authorize",
          security: [{ bearerAuth: [] }],
          parameters: [
            {
              name: "_id",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          requestBody: {
            description: "Change password",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/UserChangePassword",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/UserChangePassword",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/UserChangePassword",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Password change",
            },
            400: {
              description: "Validation error",
            },
            403: {
              description: "Not access",
            },
          },
        },
      },
      "/auth/change-password/student/{_id}": {
        post: {
          tags: ["Change password users"],
          summary: "Change password Student",
          description: "Need paste JWT Token in Authorize",
          security: [{ bearerAuth: [] }],
          parameters: [
            {
              name: "_id",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          requestBody: {
            description: "Change password",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/StudentChangePassword",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/StudentChangePassword",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/StudentChangePassword",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Password change",
            },
            400: {
              description: "Validation error",
            },
            403: {
              description: "Not access",
            },
          },
        },
      },
      "/teacher": {
        get: {
          tags: ["Teachers"],
          summary: "getAllTeacher",
          responses: {
            200: {
              description: "Return Teachers",
            },
          },
        },
      },
      "/teacher/{id}": {
        get: {
          tags: ["Teachers"],
          summary: "getOne",
          parameters: [
            {
              name: "id",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return Teacher",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/teacher/{userId}": {
        put: {
          tags: ["Teachers"],
          summary: "addPermission",
          parameters: [
            {
              name: "userId",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          requestBody: {
            description: "Add subjects",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/UserPermission",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/UserPermission",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/UserPermission",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Success",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/teacher/{userId}/{permissionId}": {
        delete: {
          tags: ["Teachers"],
          summary: "deletePermission",
          parameters: [
            {
              name: "userId",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "permissionId",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Delete succefully",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/delete/teacher/{userId}": {
        delete: {
          tags: ["Teachers"],
          summary: "kickTeacher",
          parameters: [
            {
              name: "userId",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Delete succefully",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/students": {
        get: {
          tags: ["Students"],
          summary: "getAllStudents",
          responses: {
            200: {
              description: "Return Students",
            },
          },
        },
      },
      "/student/{id}": {
        get: {
          tags: ["Students"],
          summary: "getOne",
          parameters: [
            {
              name: "id",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return Student",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/students/{classId}": {
        get: {
          tags: ["Students"],
          summary: "getByClass",
          parameters: [
            {
              name: "classId",
              in: "path",
              description: "ID class",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return Students by Class ID",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/student/changeClass/{id}": {
        put: {
          tags: ["Students"],
          summary: "changeClassStudent",
          parameters: [
            {
              name: "id",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          requestBody: {
            description: "Add subjects",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/StudentClass",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/StudentClass",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/StudentClass",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Success",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/student/{studentId}": {
        delete: {
          tags: ["Students"],
          summary: "kickStudent",
          parameters: [
            {
              name: "studentId",
              in: "path",
              description: "ID user",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Delete succefully",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/class": {
        post: {
          tags: ["Classes"],
          summary: "createClass",
          requestBody: {
            description: "Add subjects",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/ClassesCreate",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/ClassesCreate",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/ClassesCreate",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Create class succefully",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/classList": {
        get: {
          tags: ["Classes"],
          summary: "getAllClasses",
          responses: {
            200: {
              description: "Response all classes",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/classList/{id}": {
        get: {
          tags: ["Classes"],
          summary: "getOneClass",
          parameters: [
            {
              name: "id",
              in: "path",
              description: "ID class",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return Class",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/subject": {
        post: {
          tags: ["Subjects"],
          summary: "createSubject",
          requestBody: {
            description: "Add subjects",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/PredmetCreate",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/PredmetCreate",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/PredmetCreate",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Create subject succefully",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/predmet": {
        get: {
          tags: ["Subjects"],
          summary: "getAllSubjects",
          responses: {
            200: {
              description: "Return all subjects",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/predmet/{predmetId}": {
        get: {
          tags: ["Subjects"],
          summary: "getOneSubject",
          parameters: [
            {
              name: "predmetId",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return Subject",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/predmet/class/{id}": {
        get: {
          tags: ["Subjects"],
          summary: "getPredmetByClass",
          parameters: [
            {
              name: "predmetId",
              in: "path",
              description: "ID class",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return Subject",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/predmet/{id}": {
        put: {
          tags: ["Subjects"],
          summary: "addClasses",
          parameters: [
            {
              name: "id",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          requestBody: {
            description: "Add classes",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/PredmetUpdateClasses",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/PredmetUpdateClasses",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/PredmetUpdateClasses",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Add classes succesufully",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/predmet/{predmetId}/{classId}": {
        delete: {
          tags: ["Subjects"],
          summary: "deleteClassPredmet",
          parameters: [
            {
              name: "predmetId",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "classId",
              in: "path",
              description: "ID class",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Delete succefully",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/delete/predmet/{predmetId}": {
        delete: {
          tags: ["Subjects"],
          summary: "deletePredmetById",
          parameters: [
            {
              name: "predmetId",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Delete succefully",
            },
            404: {
              description: "Not found",
            },
          },
        },
      },
      "/ktp": {
        post: {
          tags: ["KTP"],
          summary: "createKtp",
          requestBody: {
            description: "Add ktp, notice only 1 ktp in period",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/KtpCreate",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/KtpCreate",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/KtpCreate",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Create ktp succefully",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/ktps": {
        get: {
          tags: ["KTP"],
          summary: "getAllKtp",
          responses: {
            200: {
              description: "Return all ktps",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/ktp/period/{classId}/{period}/{subjectId}": {
        get: {
          tags: ["KTP"],
          summary: "getByPeriod",
          parameters: [
            {
              name: "classId",
              in: "path",
              description: "ID class",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "period",
              in: "path",
              description: "Period",
              required: true,
              schema: {
                type: "string",
                example: "1 || 2 || 3 || 4",
              },
            },
            {
              name: "subjectId",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return all ktps by period, subject, class",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/ktps/{ktpId}": {
        get: {
          tags: ["KTP"],
          summary: "getOneById",
          parameters: [
            {
              name: "ktpId",
              in: "path",
              description: "ID KTP",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return ktp by ID",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/ktp/{predmetId}/{classId}": {
        get: {
          tags: ["KTP"],
          summary: "getOneById",
          parameters: [
            {
              name: "predmetId",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "classId",
              in: "path",
              description: "ID class",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return all ktps class, subject",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/ktps/my/{predmetId}/{classId}": {
        get: {
          tags: ["KTP"],
          summary: "getByMyClassByPredmet",
          parameters: [
            {
              name: "predmetId",
              in: "path",
              description: "ID subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "classId",
              in: "path",
              description: "ID class",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return all ktps class, subject",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/ktp/{ktpID}": {
        delete: {
          tags: ["KTP"],
          summary: "deleteKtp with Mark",
          parameters: [
            {
              name: "ktpID",
              in: "path",
              description: "ID KTP",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Delete succefully",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/marks": {
        post: {
          tags: ["Marks"],
          summary: "createMark",
          requestBody: {
            description: "Add mark",
            content: {
              "application/json": {
                schema: {
                  $ref: "#/components/schemas/MarkCreate",
                },
              },
              "application/xml": {
                schema: {
                  $ref: "#/components/schemas/MarkCreate",
                },
              },
              "application/x-www-form-urlencoded": {
                schema: {
                  $ref: "#/components/schemas/MarkCreate",
                },
              },
            },
            required: true,
          },
          responses: {
            200: {
              description: "Create Mark succefully",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/marksall": {
        get: {
          tags: ["Marks"],
          summary: "getAllMarks",
          responses: {
            200: {
              description: "Return all marks",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/marks/{markId}": {
        get: {
          tags: ["Marks"],
          summary: "getOneByID",
          parameters: [
            {
              name: "markId",
              in: "path",
              description: "ID Mark",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return mark",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/mark/{studentId}/{ktpId}": {
        get: {
          tags: ["Marks"],
          summary: "getByStudentByKtp",
          parameters: [
            {
              name: "studentId",
              in: "path",
              description: "ID Student",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "ktpId",
              in: "path",
              description: "ID KTP",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Return mark",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/marks/final/{studentId}/{predmetId}/{type}/{period}": {
        get: {
          tags: ["Marks"],
          summary: "getByStudentByPredmetByPeriod",
          parameters: [
            {
              name: "studentId",
              in: "path",
              description: "ID Student",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "predmetId",
              in: "path",
              description: "ID Subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "type",
              in: "path",
              description: "Type subject",
              required: true,
              schema: {
                type: "string",
                example: "sor || soch || default",
              },
            },
            {
              name: "period",
              in: "path",
              description: "Period",
              required: true,
              schema: {
                type: "string",
                example: "1 || 2 || 3 || 4",
              },
            },
          ],
          responses: {
            200: {
              description: "Return mark",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/marks/all/{studentId}/{predmetId}/{period}": {
        get: {
          tags: ["Marks"],
          summary: "getAllMarkStudent",
          parameters: [
            {
              name: "studentId",
              in: "path",
              description: "ID Student",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "predmetId",
              in: "path",
              description: "ID Subject",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "period",
              in: "path",
              description: "Period",
              required: true,
              schema: {
                type: "string",
                example: "1 || 2 || 3 || 4",
              },
            },
          ],
          responses: {
            200: {
              description: "Return marks",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
      "/marks/{studentId}/{ktpId}": {
        delete: {
          tags: ["Marks"],
          summary: "deleteMark",
          parameters: [
            {
              name: "studentId",
              in: "path",
              description: "ID Student",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
            {
              name: "ktpId",
              in: "path",
              description: "ID Ktp",
              required: true,
              schema: {
                type: "string",
                example: "63fdbb3c01e0f48bbccb770a",
              },
            },
          ],
          responses: {
            200: {
              description: "Delete succefully",
            },
            500: {
              description: "Server error",
            },
          },
        },
      },
    },
    components: {
      securitySchemes: {
        bearerAuth: {
          type: "http",
          scheme: "bearer",
          bearerFormat: "JWT",
        },
      },
      schemas: {
        Classes: {
          type: "object",
          properties: {
            _id: {
              type: "ObjectId",
              format: "ObjectId",
              example: "63fdbb3c01e0f48bbccb770a",
            },
            className: {
              type: "string",
              example: "Class name",
              required: true,
            },
          },
          xml: {
            name: "classes",
          },
        },
        ClassesCreate: {
          type: "object",
          properties: {
            className: {
              type: "string",
              example: "Class name",
              required: true,
            },
          },
          xml: {
            name: "classescreate",
          },
        },
        Ktp: {
          type: "object",
          properties: {
            _id: {
              type: "ObjectId",
              format: "ObjectId",
              example: "63fdbb3c01e0f48bbccb770a",
            },
            ktpTitle: {
              type: "string",
              example: "Name lesson",
              required: true,
            },
            ktpDate: {
              type: "string",
              example: "2023-04-03(yyyy-mm-dd)",
              required: true,
            },
            ktpPredmet: {
              type: "string",
              example: "id subject - (643b0a2bb776cf7e838bb565)",
              required: true,
            },
            ktpClass: {
              type: "string",
              example: "id class students - (63fdbb3c01e0f48bbccb770a)",
              required: true,
            },
            ktpTeacher: {
              type: "string",
              example: "id userTeacher - (643db66a96aa9917ae738b54)",
              required: true,
            },
            ktpSorSoch: {
              type: "string",
              example: "sor || soch || default | Only 1 soch in period",
              required: true,
            },
            ktpMaxValue: {
              type: "int32",
              example: "(only from 10 to 30)",
              required: true,
            },
            ktpPeriod: {
              type: "string",
              example: "1 || 2 || 3 || 4",
              required: true,
            },
          },
          xml: {
            name: "ktp",
          },
        },
        KtpCreate: {
          type: "object",
          properties: {
            ktpTitle: {
              type: "string",
              example: "Name lesson",
              required: true,
            },
            ktpDate: {
              type: "string",
              example: "2023-04-03(yyyy-mm-dd)",
              required: true,
            },
            ktpPredmet: {
              type: "string",
              example: "id subject - (643b0a2bb776cf7e838bb565)",
              required: true,
            },
            ktpClass: {
              type: "string",
              example: "id class students - (63fdbb3c01e0f48bbccb770a)",
              required: true,
            },
            ktpTeacher: {
              type: "string",
              example: "id userTeacher - (643db66a96aa9917ae738b54)",
              required: true,
            },
            ktpSorSoch: {
              type: "string",
              example: "sor || soch || default | Only 1 soch in period",
              required: true,
            },
            ktpMaxValue: {
              type: "int32",
              example: "(only from 10 to 30)",
              required: true,
            },
            ktpPeriod: {
              type: "string",
              example: "1 || 2 || 3 || 4",
              required: true,
            },
          },
          xml: {
            name: "ktpcreate",
          },
        },
        Mark: {
          type: "object",
          properties: {
            _id: {
              type: "ObjectId",
              format: "ObjectId",
              example: "63fdbb3c01e0f48bbccb770a",
            },
            markTeacher: {
              type: "string",
              example: "Is id userTeacher - (643db66a96aa9917ae738b54)",
              required: true,
            },
            markPredmet: {
              type: "string",
              example: "Is id subject - (643d5467d0e06b0236d8303e)",
              required: true,
            },
            markStudent: {
              type: "string",
              example: "Is id student - (643db6f696aa9917ae738b58)",
              required: true,
            },
            markClassStudent: {
              type: "string",
              example: "Is id class - (63fdbb3c01e0f48bbccb770a)",
              required: true,
            },
            markDate: {
              type: "string",
              example: "Is id ktp - (643db78e96aa9917ae738b77)",
              required: true,
            },
            markFalse: {
              type: "boolean",
              example:
                "When a student missed a lesson then true, in all other cases false",
              required: true,
            },
            markMaxValue: {
              type: "int32",
              example: "(only from 10 to 30) value from ktp",
              required: true,
            },
            markValue: {
              type: "int32",
              example: "(only from 10 to 30) teacher set mark",
              required: true,
            },
            markSochSor: {
              type: "string",
              example: "sor || soch || default",
              required: true,
            },
            markPeriod: {
              type: "string",
              example: "1 || 2 || 3 || 4",
              required: true,
            },
          },
          xml: {
            name: "mark",
          },
        },
        MarkCreate: {
          type: "object",
          properties: {
            markTeacher: {
              type: "string",
              example: "Is id userTeacher - (643db66a96aa9917ae738b54)",
              required: true,
            },
            markPredmet: {
              type: "string",
              example: "Is id subject - (643d5467d0e06b0236d8303e)",
              required: true,
            },
            markStudent: {
              type: "string",
              example: "Is id student - (643db6f696aa9917ae738b58)",
              required: true,
            },
            markClassStudent: {
              type: "string",
              example: "Is id class - (63fdbb3c01e0f48bbccb770a)",
              required: true,
            },
            markDate: {
              type: "string",
              example: "Is id ktp - (643db78e96aa9917ae738b77)",
              required: true,
            },
            markFalse: {
              type: "boolean",
              example:
                "When a student missed a lesson then true, in all other cases false",
              required: true,
            },
            markMaxValue: {
              type: "int32",
              example: "(only from 10 to 30) value from ktp",
              required: true,
            },
            markValue: {
              type: "int32",
              example: "(only from 10 to 30) teacher set mark",
              required: true,
            },
            markSochSor: {
              type: "string",
              example: "sor || soch || default",
              required: true,
            },
            markPeriod: {
              type: "string",
              example: "1 || 2 || 3 || 4",
              required: true,
            },
          },
          xml: {
            name: "markcreate",
          },
        },
        Predmet: {
          type: "object",
          properties: {
            _id: {
              type: "ObjectId",
              format: "ObjectId",
              example: "63fdbb3c01e0f48bbccb770a",
            },
            predmetName: {
              type: "string",
              example: "Math",
              required: true,
            },
            classes: {
              type: "array",
              items: {
                $ref: "#/components/schemas/Classes",
              },
              required: true,
            },
          },
          xml: {
            name: "predmet",
          },
        },
        PredmetCreate: {
          type: "object",
          properties: {
            predmetName: {
              type: "string",
              example: "Math",
              required: true,
            },
            classes: {
              type: "array",
              items: {
                $ref: "#/components/schemas/Classes",
              },
              required: true,
            },
          },
        },
        PredmetUpdateClasses: {
          type: "object",
          properties: {
            classes: {
              type: "array",
              items: {
                $ref: "#/components/schemas/Classes",
              },
              required: true,
            },
          },
        },
        User: {
          type: "object",
          properties: {
            _id: {
              type: "ObjectId",
              format: "ObjectId",
              example: "63fdbb3c01e0f48bbccb770a",
            },
            fullName: {
              type: "string",
              example: "Name Surname",
              required: true,
            },
            email: {
              type: "string",
              example: "example@mail.domain",
              required: true,
              unique: true,
            },
            passwordHash: {
              type: "string",
              example: "create when you register userTeacher",
              required: true,
            },
            permission: {
              type: "array",
              items: {
                type: "object",
                required: ["_id", "predmetName"],
                properties: {
                  _id: {
                    type: "string",
                  },
                  predmetName: {
                    type: "string",
                  },
                },
              },
              required: true,
            },
            avatarUrl: {
              type: "string",
              format: "string",
              example: "link photo (cooming soon)",
              required: false,
            },
          },
          xml: {
            name: "user",
          },
        },
        Student: {
          type: "object",
          properties: {
            _id: {
              type: "ObjectId",
              format: "ObjectId",
              example: "63fdbb3c01e0f48bbccb770a",
            },
            fullName: {
              type: "string",
              example: "Name Surname",
              required: true,
            },
            email: {
              type: "string",
              example: "example@mail.domain",
              required: true,
              unique: true,
            },
            passwordHash: {
              type: "string",
              example: "create when you register userTeacher",
              required: true,
            },
            classId: {
              type: "string",
              example: "id class - 63fdbb3c01e0f48bbccb770a",
              required: true,
            },
            avatarUrl: {
              type: "string",
              format: "string",
              example: "link photo (cooming soon)",
              required: false,
            },
          },
          xml: {
            name: "student",
          },
        },
        UserRegister: {
          type: "object",
          properties: {
            fullName: {
              type: "string",
              example: "Name Surname",
              required: true,
            },
            email: {
              type: "string",
              example: "example@mail.domain",
              required: true,
              unique: true,
            },
            password: {
              type: "string",
              example: "12345",
              required: true,
            },
            permission: {
              type: "array",
              items: {
                type: "object",
                required: ["_id", "predmetName"],
                properties: {
                  _id: {
                    type: "string",
                  },
                  predmetName: {
                    type: "string",
                  },
                },
              },
              required: true,
            },
          },
          xml: {
            name: "userregister",
          },
        },
        UserLogin: {
          type: "object",
          properties: {
            email: {
              type: "string",
              example: "example@mail.domain",
              required: true,
              unique: true,
            },
            password: {
              type: "string",
              example: "12345",
              required: true,
            },
          },
          xml: {
            name: "userlogin",
          },
        },
        StudentRegister: {
          type: "object",
          properties: {
            fullName: {
              type: "string",
              example: "Name Surname",
              required: true,
            },
            email: {
              type: "string",
              example: "example@mail.domain",
              required: true,
              unique: true,
            },
            password: {
              type: "string",
              example: "12345",
              required: true,
            },
            classId: {
              type: "string",
              example: "63fdbb3c01e0f48bbccb770a",
              required: true,
            },
          },
          xml: {
            name: "studentregister",
          },
        },
        StudentLogin: {
          type: "object",
          properties: {
            email: {
              type: "string",
              example: "example@mail.domain",
              required: true,
              unique: true,
            },
            password: {
              type: "string",
              example: "12345",
              required: true,
            },
          },
          xml: {
            name: "studentlogin",
          },
        },
        UserChangePassword: {
          type: "object",
          properties: {
            oldPassword: {
              type: "string",
              example: "12345",
              required: true,
            },
            newPassword: {
              type: "string",
              example: "12345",
              required: true,
            },
          },
          xml: {
            name: "userchangepassword",
          },
        },
        StudentChangePassword: {
          type: "object",
          properties: {
            oldPassword: {
              type: "string",
              example: "12345",
              required: true,
            },
            newPassword: {
              type: "string",
              example: "12345",
              required: true,
            },
          },
          xml: {
            name: "studentchangepassword",
          },
        },
        UserPermission: {
          type: "object",
          properties: {
            permission: {
              type: "array",
              items: {
                type: "object",
                required: ["_id", "predmetName"],
                properties: {
                  _id: {
                    type: "string",
                  },
                  predmetName: {
                    type: "string",
                  },
                },
              },
              required: true,
            },
          },
          xml: {
            name: "userpermission",
          },
        },
        StudentClass: {
          type: "object",
          properties: {
            classId: {
              type: "string",
              example: "63fdbb3c01e0f48bbccb770a",
              required: true,
            },
          },
          xml: {
            name: "studentclass",
          },
        },
      },
    },
  },
  apis: [],
};
