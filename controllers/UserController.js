import jwt from "jsonwebtoken";
import bcrypt from "bcrypt";
import i18n from "i18n";
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
    res.status(500).json({
      message: i18n.__("registerFailed"),
    });
  }
};

export const login = async (req, res) => {
  try {
    const user = await UserModel.findOne({ email: req.body.email });

    if (!user) {
      return res.status(404).json({
        message: i18n.__("userNotFound"),
      });
    }

    const isValidPass = await bcrypt.compare(
      req.body.password,
      user._doc.passwordHash
    );

    if (!isValidPass) {
      return res.status(400).json({
        message: i18n.__("loginPasswordFailed"),
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
    res.status(500).json({
      message: i18n.__("loginFailed"),
    });
  }
};

export const getAllTeacher = async (req, res) => {
  try {
    const teachers = await UserModel.find().exec();
    res.json(teachers);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("teacherListGetError"),
    });
  }
};

export const getMe = async (req, res) => {
  try {
    const user = await UserModel.findById(req.userId);

    if (!user) {
      return res.status(404).json({
        message: i18n.__("userNotFound"),
      });
    }

    const { passwordHash, ...userData } = user._doc;

    res.json(userData);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("not"),
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
      return res.status(400).json({ message: i18n.__("passwordWrong") });
    }
    if (newPassword.length < 5) {
      return res.status(400).json({ message: i18n.__("passwordChangeValid") });
    }

    const salt = await bcrypt.genSalt(10);
    const newPasswordHash = await bcrypt.hash(newPassword, salt);

    user.passwordHash = newPasswordHash;
    await user.save();
    res.status(200).json({ message: i18n.__("passwordChange") });
  } catch (error) {
    res.status(500).json({ message: i18n.__("passwordChangeError") });
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
            message: i18n.__("teacherError"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("teacherError"),
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("teacherError"),
    });
  }
};

export const deleteClassPredmet = async (req, res) => {
  const { id, permissionId } = req.params;

  try {
    const teacher = await UserModel.findById(id);

    if (!teacher) {
      return res.status(404).json({ message: i18n.__("teacherError") });
    }
    const permissionIndex = teacher.permission.findIndex(
      (data) => data._id == permissionId
    );

    if (permissionIndex === -1) {
      return res.status(404).json({ error: i18n.__("subjectNotFound") });
    }

    teacher.permission.splice(permissionIndex, 1);
    await teacher.save();

    res.status(200).json({ message: i18n.__("subjectDeletedSuccessfully") });
  } catch (error) {
    return res.status(500).json({ message: i18n.__("serverError") });
  }
};

export const addPermission = async (req, res) => {
  const { permission } = req.body;
  const id = req.params.id;

  try {
    const teacher = await UserModel.findById(id);

    if (!teacher) {
      return res.status(404).json({ message: i18n.__("teacherError") });
    }

    const newPermission = permission.filter(
      (c) => !teacher.permission.some((pc) => pc._id === c._id)
    );

    if (newPermission.length === 0) {
      return res.json({ message: i18n.__("subjectTeacherCreateErrorAdd") });
    }

    teacher.permission.push(...newPermission);
    await teacher.save();

    return res.json({
      message: i18n.__("subjectTeacherCreateSuccefullyAdded"),
    });
  } catch (error) {
    return res.status(500).json({ message: i18n.__("serverError") });
  }
};

export const deleteTeacher = async (req, res) => {
  try {
    const id = req.params.id;
    await UserModel.findByIdAndRemove(id);
    res.status(204).json({ message: i18n.__("teacherDelete") });
  } catch (error) {
    res.status(500).json({ message: i18n.__("serverError") });
  }
};
