import jwt from 'jsonwebtoken';
import bcrypt from 'bcrypt';

import StudentModel from '../models/Student.js';

export const register = async (req, res) => {
  try {
    const password = req.body.password;
    const salt = await bcrypt.genSalt(10);
    const hash = await bcrypt.hash(password, salt);

    const doc = new StudentModel({
      email: req.body.email,
      fullName: req.body.fullName,
      avatarUrl: req.body.avatarUrl,
      classId: req.body.classId,
      passwordHash: hash,
    });

    const user = await doc.save();

    const token = jwt.sign(
      {
        _id: user._id,
      },
      'secret123',
      {
        expiresIn: '30d',
      },
    );

    const { passwordHash, ...userData } = user._doc;

    res.json({
      ...userData,
      token,
    });
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось зарегистрироваться',
    });
  }
};

export const login = async (req, res) => {
  try {
    const user = await StudentModel.findOne({ email: req.body.email });

    if (!user) {
      return res.status(404).json({
        message: 'Пользователь не найден',
      });
    }

    const isValidPass = await bcrypt.compare(req.body.password, user._doc.passwordHash);

    if (!isValidPass) {
      return res.status(400).json({
        message: 'Неверный логин или пароль',
      });
    }

    const token = jwt.sign(
      {
        _id: user._id,
      },
      'secret123',
      {
        expiresIn: '30d',
      },
    );

    const { passwordHash, ...userData } = user._doc;

    res.json({
      ...userData,
      token,
    });
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось авторизоваться',
    });
  }
};

export const getMe = async (req, res) => {
  try {
    const user = await StudentModel.findById(req.userId);

    if (!user) {
      return res.status(404).json({
        message: 'Студент не найден',
      });
    }

    const { passwordHash, ...userData } = user._doc;

    res.json(userData);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Нет доступа',
    });
  }
};

export const changePassword = async (req, res) => {
  const { oldPassword, newPassword } = req.body;
  const userId = req.params._id;
  try {
    const user = await StudentModel.findById(userId);

    const isOldPasswordCorrect = await bcrypt.compare(oldPassword, user.passwordHash);
    if (!isOldPasswordCorrect) {
      return res.status(400).json({ message: 'Не верный пароль' });
    }
    if(newPassword.length < 5) {
      return res.status(400).json({ message: 'Пароль должен быть больше 5 символов' });
    }

    const salt = await bcrypt.genSalt(10);
    const newPasswordHash = await bcrypt.hash(newPassword, salt);

    user.passwordHash = newPasswordHash;
    await user.save();
    res.status(200).json({ message: "Пароль изменен"})
  }
  catch(error) {
    res.status(500).json({ message: 'Не возможно изменить пароль' });
  }
}

export const getAllStudents = async (req, res) => {
  try {
    const students = await StudentModel.find().exec();
    res.json(students);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить студентов',
    });
  }
};

export const getStudentsByClass = async (req, res) => {
  try {
    const students = await StudentModel.find(
      {
        classId: req.params.classId,
      },
    ).exec();
    res.json(students);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить список студентов по классу',
    });
  }
};

export const getOne = async (req, res) => {
  try {
    const student = req.params.id;

    StudentModel.findOneAndUpdate(
      {
        _id: student,
      },
      {
        $inc: { viewsCount: 1 },
      },
      {
        returnDocument: 'after',
      },
      (err, doc) => {
        if (err) {
          return res.status(500).json({
            message: 'Не удалось вернуть студента',
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: 'Студент не найден',
          });
        }

        res.json(doc);
      },
    )
  } catch (err) {
    res.status(500).json({
      message: 'Не удалось получить студента',
    });
  }
};