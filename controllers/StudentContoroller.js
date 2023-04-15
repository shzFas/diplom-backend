import jwt from "jsonwebtoken";
import bcrypt from "bcrypt";
import i18n from "i18n";
import StudentModel from "../models/Student.js";

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
    const user = await StudentModel.findOne({ email: req.body.email });

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

export const getMe = async (req, res) => {
  try {
    const user = await StudentModel.findById(req.userId);

    if (!user) {
      return res.status(404).json({
        message: i18n.__("studentError"),
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
    const user = await StudentModel.findById(userId);

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

export const getAllStudents = async (req, res) => {
  try {
    const students = await StudentModel.find().exec();
    res.json(students);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("studentListError"),
    });
  }
};

export const getStudentsByClass = async (req, res) => {
  try {
    const students = await StudentModel.find({
      classId: req.params.classId,
    }).exec();
    res.json(students);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("studentsListByClass"),
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
        returnDocument: "after",
      },
      (err, doc) => {
        if (err) {
          return res.status(500).json({
            message: i18n.__("studentError"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("studentError"),
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("serverError"),
    });
  }
};

export const deleteStudent = async (req, res) => {
  try {
    const studentId = req.params.id;
    await StudentModel.findByIdAndRemove(studentId);
    res.status(200).json({ message: i18n.__("studentDelete") });
  } catch (error) {
    res.status(500).json({ message: i18n.__("serverError") });
  }
};

export const changeClassStudent = async (req, res) => {
  try {
    const studentId = req.params.id;
    const { classId } = req.body;
    const updatedStudent = await StudentModel.findByIdAndUpdate(
      studentId,
      { classId },
      { new: true }
    );
    res.status(200).json({ message: i18n.__("studentClassChange") });
  } catch (error) {
    res.status(500).json({ message: i18n.__("serverError") });
  }
};
