import express from 'express';
import cors from 'cors';
import mongoose from 'mongoose';
import { UserController } from './controllers/index.js';
import { loginValidation, registerValidation } from './validations.js';
import { handleValidationErrors, checkAuth } from './utils/index.js';

mongoose
  .connect(`ССЫЛКА НА МОНГУ`)
  .then(() => console.log('DB ok'))
  .catch((err) => console.log('DB error', err));

const app = express();
app.use(express.json());
app.use(cors());

/* Регистрация учителя */
app.post('/auth/login', loginValidation, handleValidationErrors, UserController.login);
app.post('/auth/register', registerValidation, handleValidationErrors, UserController.register);
app.get('/auth/me', checkAuth, UserController.getMe);

app.listen(process.env.PORT || 4444, (err) => {
  if (err) {
    return console.log(err);
  }
  console.log('Server OK');
});
