export const options = {
  definition: {
    openapi: "3.0.3",
    info: {
      title: "Diplom Server",
      contact: {
        url: "github.com/shzfas",
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
        description: "CRUD teachers"
      },
      {
        name: "Students",
        description: "CRUD students"
      }
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
          parameters: [{
            name: "_id",
            in: "path",
            description: "ID user",
            required: true,
            schema: {
              type: "string",
              example: "63fdbb3c01e0f48bbccb770a",
            }
          }],
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
        }
      },
      "/auth/change-password/student/{_id}": {
        post: {
          tags: ["Change password users"],
          summary: "Change password Student",
          description: "Need paste JWT Token in Authorize",
          security: [{ bearerAuth: [] }],
          parameters: [{
            name: "_id",
            in: "path",
            description: "ID user",
            required: true,
            schema: {
              type: "string",
              example: "63fdbb3c01e0f48bbccb770a",
            }
          }],
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
        }
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
        }
      },
      "/teacher/{id}": {
        get: {
          tags: ["Teachers"],
          summary: "getOne",
          parameters: [{
            name: "id",
            in: "path",
            description: "ID user",
            required: true,
            schema: {
              type: "string",
              example: "63fdbb3c01e0f48bbccb770a",
            }
          }],
          responses: {
            200: {
              description: "Return Teachers",
            },
            404: {
              description: "Not found"
            }
          },
        }
      }
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
              unique: true,
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
      },
    },
  },
  apis: [],
};
