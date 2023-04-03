import jwt from "jsonwebtoken";
import bcrypt from "bcrypt";

import UserModel from "../models/User.js";

export const register = async (req, res) => {
  try {
    const password = req.body.password;
    const salt = await bcrypt.genSalt(10);
    const hash = await bcrypt.hash(password, salt);

    const doc = new UserModel({
      email: req.body.email,
      fullName: req.body.fullName,
      avatarUrl: req.body.avatarUrl,
      permission: req.body.permission,
      passwordHash: hash,
    });

    const user = await doc.save();

    const token = jwt.sign(
      {
        _id: user._id,
      },
      "secret123",
      {
        expiresIn: "30d",
      }
    );

    const { passwordHash, ...userData } = user._doc;

    res.json({
      ...userData,
      token,
    });
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: "Не удалось зарегистрироваться",
    });
  }
};

export const login = async (req, res) => {
  try {
    const user = await UserModel.findOne({ email: req.body.email });

    if (!user) {
      return res.status(404).json({
        message: "Пользователь не найден",
      });
    }

    const isValidPass = await bcrypt.compare(
      req.body.password,
      user._doc.passwordHash
    );

    if (!isValidPass) {
      return res.status(400).json({
        message: "Неверный логин или пароль",
      });
    }

    const token = jwt.sign(
      {
        _id: user._id,
      },
      "secret123",
      {
        expiresIn: "30d",
      }
    );

    const { passwordHash, ...userData } = user._doc;

    res.json({
      ...userData,
      token,
    });
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: "Не удалось авторизоваться",
    });
  }
};

export const getAllTeacher = async (req, res) => {
  try {
    const teachers = await UserModel.find().exec();
    res.json(teachers);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: "Не удалось получить список учителей",
    });
  }
};

export const getMe = async (req, res) => {
  try {
    const user = await UserModel.findById(req.userId);

    if (!user) {
      return res.status(404).json({
        message: "Пользователь не найден",
      });
    }

    const { passwordHash, ...userData } = user._doc;

    res.json(userData);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: "Нет доступа",
    });
  }
};

export const changePassword = async (req, res) => {
  const { oldPassword, newPassword } = req.body;
  const userId = req.params._id;
  try {
    const user = await UserModel.findById(userId);

    const isOldPasswordCorrect = await bcrypt.compare(
      oldPassword,
      user.passwordHash
    );
    if (!isOldPasswordCorrect) {
      return res.status(400).json({ message: "Не верный пароль" });
    }
    if (newPassword.length < 5) {
      return res
        .status(400)
        .json({ message: "Пароль должен быть больше 5 символов" });
    }

    const salt = await bcrypt.genSalt(10);
    const newPasswordHash = await bcrypt.hash(newPassword, salt);

    user.passwordHash = newPasswordHash;
    await user.save();
    res.status(200).json({ message: "Пароль изменен" });
  } catch (error) {
    res.status(500).json({ message: "Не возможно изменить пароль" });
  }
};

export const getOne = async (req, res) => {
  try {
    const teacher = req.params.id;

    UserModel.findOneAndUpdate(
      {
        _id: teacher,
      },
      {
        $inc: { viewsCount: 1 },
      },
      {
        returnDocument: "after",
      },
      (err, doc) => {
        if (err) {
          return res.status(500).json({
            message: "Не удалось вернуть учителя",
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: "Учитель не найден",
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    res.status(500).json({
      message: "Не удалось получить учителя",
    });
  }
};

export const deleteClassPredmet = async (req, res) => {
  const { id, permissionId } = req.params;

  try {
    const teacher = await UserModel.findById(id);

    if (!teacher) {
      return res.status(404).json({ message: "Учитель не найден" });
    }
    const permissionIndex = teacher.permission.findIndex(
      (data) => data._id == permissionId
    );

    if (permissionIndex === -1) {
      return res.status(404).json({ error: "Предмет не найден" });
    }

    teacher.permission.splice(permissionIndex, 1);
    await teacher.save();

    res.status(200).json({ message: "Предмет удален" });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Ошибка сервера" });
  }
};

export const addPermission = async (req, res) => {
  const { permission } = req.body;
  const id = req.params.id;

  try {
    const teacher = await UserModel.findById(id);

    if (!teacher) {
      return res.status(404).json({ message: "Учитель не найден" });
    }

    const newPermission = permission.filter(
      (c) => !teacher.permission.some((pc) => pc._id === c._id)
    );

    if (newPermission.length === 0) {
      return res.json({ message: "Новые предметы не добавлены" });
    }

    teacher.permission.push(...newPermission);
    await teacher.save();

    return res.json({ message: "Предметы успешно добавлены" });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: "Ошибка сервера" });
  }
};
