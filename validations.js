import { body } from 'express-validator';

export const loginValidation = [
  body('email', 'Неверный формат почты').isEmail(),
  body('password', 'Пароль должен быть минимум 5 символов').isLength({ min: 5 }),
];

export const registerValidation = [
  body('email', 'Неверный формат почты').isEmail(),
  body('password', 'Пароль должен быть минимум 5 символов').isLength({ min: 5 }),
  body('fullName', 'Укажите имя').isLength({ min: 3 }),
  body('permission', 'Укажите предмет').isLength({ min: 3 }),
  body('avatarUrl', 'Неверная ссылка на аватарку').optional().isURL(),
];

export const loginStudentValidation = [
  body('email', 'Неверный формат почты').isEmail(),
  body('password', 'Пароль должен быть минимум 5 символов').isLength({ min: 5 }),
];

export const registerStudentValidation = [
  body('email', 'Неверный формат почты').isEmail(),
  body('password', 'Пароль должен быть минимум 5 символов').isLength({ min: 5 }),
  body('fullName', 'Укажите имя').isLength({ min: 3 }),
  body('classId', 'Укажите класс').isLength({ min: 3 }),
  body('avatarUrl', 'Неверная ссылка на аватарку').optional().isURL(),
];

export const classCreateValidation = [
  body('className', 'Введите название класса').isLength({ min: 2 }).isString(),
];

export const predmetValidation = [
  body('predmetName', 'Введите название предмета').isLength({ min: 2 }).isString(),
  body('classes', 'Добавьте классы в предмет').isArray(),
];

export const ktpValidation = [
  body('ktpTitle', 'Введите тему урока').isLength({ min: 2 }).isString(),
  body('ktpDate', 'Введите дату урока').isLength({ min: 2 }).isString(),
  body('ktpPredmet', 'Введите id предмета').isLength({ min: 2 }).isString(),
  body('ktpClass', 'Введите id Класса').isLength({ min: 2 }).isString(),
  body('ktpTeacher', 'Введите id Учителя').isLength({ min: 2 }).isString(),
  body('ktpSorSoch', 'Введите тип урока').isLength({ min: 2 }).isString(),
  body('ktpMaxValue', 'Введите максимальный балл от 0 до 30').isNumeric(),
  body('ktpPeriod', 'Укажите четверть').isLength({ max: 1 }).isString(),
];

export const markValidation = [
  body('markTeacher', 'Введите id Преподавателя').isLength({ min: 2 }).isString(),
  body('markPredmet', 'Введите id Предмета').isLength({ min: 2 }).isString(),
  body('markStudent', 'Введите id ученика').isLength({ min: 2 }).isString(),
  body('markClassStudent', 'Введите id Класса').isLength({ min: 2 }).isString(),
  body('markDate', 'Введите id плана(дата урока его ID)').isLength({ min: 2 }).isString(),
  body('markFalse', 'true || false').isBoolean(),
  body('markMaxValue', 'от 0 до 30').isNumeric(),
  body('markValue', 'от 0 до 30').isNumeric(),
  body('markPeriod', 'Укажите четверть').isLength({ max: 1 }).isString(),
];