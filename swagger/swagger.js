import swaggerJsdoc from "swagger-jsdoc";
import swaggerUi from "swagger-ui-express";

const options = {
    definition: {
      openapi: "3.0.0",
      info: {
        title: "Documentation Diplom",
        version: "1.0.0",
      },
      components: {
        securitySchemas: {
          bearerAuth: {
            type: "http",
            scheme: "bearer",
            bearerFormat: "JWT",
          },
        },
      },
      security: [
        {
          bearerAuth: [],
        },
      ],
    },
    apis: ["../index.js", "../models/*.js"],
};

const swaggerSpec = swaggerJsdoc(options);

export const swaggerDocs = (app, port) => {
    app.use("/docs", swaggerUi.serve, swaggerUi.setup(swaggerSpec));

    app.get("/docs.json", (req, res) => {
        res.setHeader("Content-Type", "application/json");
        res.send(swaggerSpec);
    });

    console.log(`Docs active - http://localhost:${port}/docs`);
}