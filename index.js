import express from 'express';
import cors from 'cors';
import fs from 'fs';
import multer from 'multer';
import mongoose from 'mongoose';
import { ClassesController, KtpController, PredmetController, StudentController, UserController } from './controllers/index.js';
import { classCreateValidation, ktpValidation, loginStudentValidation, loginValidation, predmetValidation, registerStudentValidation, registerValidation } from './validations.js';
import { handleValidationErrors, checkAuth } from './utils/index.js';

mongoose
  .connect(``)
  .then(() => console.log('DB ok'))
  .catch((err) => console.log('DB error', err));

const app = express();

const storage = multer.diskStorage({
  destination: (_, __, cb) => {
    if (!fs.existsSync('uploads')) {
      fs.mkdirSync('uploads');
    }
    cb(null, 'uploads');
  },
  filename: (_, file, cb) => {
    cb(null, file.originalname);
  },
});

const upload = multer({ storage });
app.use(express.json());
app.use(cors());
app.use('/uploads', express.static('uploads'));

/* Регистрация учителя */
app.post('/auth/login', loginValidation, handleValidationErrors, UserController.login);
app.post('/auth/register', registerValidation, handleValidationErrors, UserController.register);
app.get('/auth/me', checkAuth, UserController.getMe);

/* Регистрация Студента */
app.post('/auth/loginStudent', loginStudentValidation, handleValidationErrors, StudentController.login);
app.post('/auth/registerStudent', registerStudentValidation, handleValidationErrors, StudentController.register);
app.get('/students', StudentController.getAllStudents);
app.get('/students/:classId', StudentController.getStudentsByClass);

/* Создание класса */
app.post('/classList', classCreateValidation, handleValidationErrors, ClassesController.createClasslist);
app.get('/classList', ClassesController.getAllClasses);
app.get('/classList/:id', ClassesController.getOne);

/* Создание предмета */
app.post('/predmet', predmetValidation, handleValidationErrors, PredmetController.createPredmet);
app.get('/predmet', PredmetController.getAllPredmets);
app.get('/predmet/:id', PredmetController.getOne);

/* Создание плана */
app.post('/ktp', ktpValidation, handleValidationErrors, KtpController.createKtp);
app.get('/ktp', KtpController.getAllKtp);
app.get('/ktp/:id', KtpController.getOne);
app.get('/ktp/:predmetId/:classId', KtpController.getByClassByPredmet);

/* Загрузка изображений на сервер */
app.post('/upload', checkAuth, upload.single('image'), (req, res) => {
  res.json({
    url: `/uploads/${req.file.originalname}`,
  });
});

app.listen(process.env.PORT || 4444, (err) => {
  if (err) {
    return console.log(err);
  }
  console.log('Server OK');
});
