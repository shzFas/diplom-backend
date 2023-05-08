import axios from "axios";
import Mark from "../models/Mark.js";
import i18n from "i18n";

export const createMark = async (req, res) => {
  try {
    const doc = new Mark({
      markTeacher: req.body.markTeacher,
      markPredmet: req.body.markPredmet,
      markStudent: req.body.markStudent,
      markClassStudent: req.body.markClassStudent,
      markDate: req.body.markDate,
      markFalse: req.body.markFalse,
      markMaxValue: req.body.markMaxValue,
      markValue: req.body.markValue,
      markSochSor: req.body.markSochSor,
      markPeriod: req.body.markPeriod,
    });
    await doc.save();
    res.status(200).json({
      message: i18n.__("markCreateSuccess"),
    });
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markCreateSuccess"),
    });
  }
};

export const createMarkWithTelegram = async (req, res) => {
  const chat_id = req.params.chat_id;
  const message = req.params.message;
  try {
    const doc = new Mark({
      markTeacher: req.body.markTeacher,
      markPredmet: req.body.markPredmet,
      markStudent: req.body.markStudent,
      markClassStudent: req.body.markClassStudent,
      markDate: req.body.markDate,
      markFalse: req.body.markFalse,
      markMaxValue: req.body.markMaxValue,
      markValue: req.body.markValue,
      markSochSor: req.body.markSochSor,
      markPeriod: req.body.markPeriod,
    });
    axios.post(`https://api.telegram.org/${process.env.TELEGRAM_BOT_TOKEN}/sendMessage?chat_id=${chat_id}&text=${message}`)
    await doc.save();
    res.status(200).json({
      message: i18n.__("markCreateSuccess"),
    });
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markCreateSuccess"),
    });
  }
};

export const getOne = async (req, res) => {
  try {
    const mark = req.params.id;

    Mark.findOneAndUpdate(
      {
        _id: mark,
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
            message: i18n.__("markNotFound"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("markNotFound"),
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markNotFound"),
    });
  }
};

export const getByStudentByKtp = async (req, res) => {
  try {
    const ktp = await Mark.find({
      markStudent: req.params.studentId,
      markDate: req.params.ktpId,
    }).exec();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markNotFound"),
    });
  }
};

export const getByStudentByPredmet = async (req, res) => {
  try {
    const ktp = await Mark.find({
      markStudent: req.params.studentId,
      markPredmet: req.params.predmetId,
      markSochSor: req.params.type,
      markPeriod: req.params.period,
    }).exec();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markGetMarksFailed"),
    });
  }
};

export const getAllMarks = async (req, res) => {
  try {
    const mark = await Mark.find().exec();
    res.json(mark);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markGetMarksFailed"),
    });
  }
};

export const deleteOne = async (req, res) => {
  try {
    const markStudentId = req.params.studentId;
    const markKtpId = req.params.ktpId;

    Mark.findOneAndDelete(
      {
        markStudent: markStudentId,
        markDate: markKtpId,
      },
      (err, doc) => {
        if (err) {
          return res.status(500).json({
            message: i18n.__("markDeleteMarkFailed"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("markNotFound"),
          });
        }

        res.json({
          message: i18n.__("deleteMark"),
        });
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markReturnMarkFailed"),
    });
  }
};

export const deleteOneTelegram = async (req, res) => {
  try {
    const markStudentId = req.params.studentId;
    const markKtpId = req.params.ktpId;
    const chat_id = req.params.chat_id;
    const message = req.params.message;

    Mark.findOneAndDelete(
      {
        markStudent: markStudentId,
        markDate: markKtpId,
      },
      (err, doc) => {
        if (err) {
          return res.status(500).json({
            message: i18n.__("markDeleteMarkFailed"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("markNotFound"),
          });
        }

        axios.post(`https://api.telegram.org/${process.env.TELEGRAM_BOT_TOKEN}/sendMessage?chat_id=${chat_id}&text=${message}`)
        
        res.json({
          message: i18n.__("deleteMark"),
        });
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markReturnMarkFailed"),
    });
  }
};

export const getAllMarkStudent = async (req, res) => {
  try {
    const ktp = await Mark.find({
      markStudent: req.params.studentId,
      markPredmet: req.params.predmetId,
      markPeriod: req.params.period,
    }).exec();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("markGetMarksFailed"),
    });
  }
};
