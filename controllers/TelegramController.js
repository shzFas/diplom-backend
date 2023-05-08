import axios from "axios";
import i18n from "i18n";
import Student from "../models/Student.js";
import User from "../models/User.js";

export const chat_idTelegramStudent = async (req, res) => {
  const studentId = req.params.studentId;
  const username = req.params.username;
  try {
    const result = await axios
      .get(
        `https://api.telegram.org/${process.env.TELEGRAM_BOT_TOKEN}/getUpdates`
      )
      .then((data) => {
        return data.data.result;
      });
    const chat_id = result.filter(
      (data) => data.message.from.username === username
    )[0].message.chat.id;
    const student = await Student.findOne({ _id: studentId });
    if (student) {
      if (chat_id) {
        student.telegram_id = chat_id;
        student.telegram_username = username;
        student.save();
        res.status(200).send(i18n.__("telegramIdSetSuccess"));
      } else {
        res.status(404).send(i18n.__("telegramIdSetError"));
      }
    } else {
      res.status(404).send(i18n.__("telegramIdSetError"));
    }
  } catch (error) {
    res.status(500).send(i18n.__("telegramIdSetError"));
  }
};

export const chat_idTelegramTeacher = async (req, res) => {
  const userId = req.params.studentId;
  const username = req.params.username;
  try {
    const result = await axios
      .get(
        `https://api.telegram.org/${process.env.TELEGRAM_BOT_TOKEN}/getUpdates`
      )
      .then((data) => {
        return data.data.result;
      });
    const chat_id = result.filter(
      (data) => data.message.from.username === username
    )[0].message.chat.id;
    const user = await User.findOne({ _id: userId });
    if (user) {
      if (chat_id) {
        user.telegram_id = chat_id;
        user.save();
        res.status(200).res(i18n.__("telegramIdSetSuccess"));
      } else {
        res.status(404).send(i18n.__("telegramIdSetError"));
      }
    } else {
      res.status(404).send(i18n.__("telegramIdSetError"));
    }
  } catch (error) {
    res.status(500).send(i18n.__("telegramIdSetError"));
  }
};
