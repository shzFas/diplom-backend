import Ktp from "../models/Ktp.js";
import Mark from "../models/Mark.js";
import i18n from "i18n";

export const createKtp = async (req, res, next) => {
  try {
    const doc = new Ktp({
      ktpTitle: req.body.ktpTitle,
      ktpDate: req.body.ktpDate,
      ktpPredmet: req.body.ktpPredmet,
      ktpClass: req.body.ktpClass,
      ktpTeacher: req.body.ktpTeacher,
      ktpSorSoch: req.body.ktpSorSoch,
      ktpMaxValue: req.body.ktpMaxValue,
      ktpPeriod: req.body.ktpPeriod,
    });
    const { ktpPredmet, ktpClass, ktpDate, ktpPeriod } = req.body;
    const ktpDB = await Ktp.find().exec();
    const duplicates = ktpDB.filter(
      (record) =>
        record.ktpPredmet === ktpPredmet &&
        record.ktpClass === ktpClass &&
        record.ktpDate === ktpDate
    );
    if (duplicates.length > 0) {
      res.status(500).json({
        message: i18n.__("ktpHaveError"),
      });
      return next();
    }
    if (req.body.ktpSorSoch === "soch") {
      const duplicatesSOCH = ktpDB.filter(
        (record) =>
          record.ktpSorSoch === "soch" &&
          record.ktpPredmet === ktpPredmet &&
          record.ktpClass === ktpClass &&
          record.ktpPeriod === ktpPeriod
      );
      if (duplicatesSOCH.length > 0) {
        res.status(500).json({
          message: i18n.__("ktpMessageErrorSOCH"),
        });
        return next();
      }
    }
    await doc.save();
    res.status(200).json({
      message: i18n.__("ktpCreateSuccess"),
    });
  } catch (err) {
    res.status(500).json({
      message: i18n.__("ktpCreateError"),
    });
  }
};

export const getOne = async (req, res) => {
  try {
    const ktp = req.params.id;

    Ktp.findOneAndUpdate(
      {
        _id: ktp,
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
            message: i18n.__("ktpReturnPlanFailed"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("ktpPlansNotFound"),
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("ktpGetPlanListFailed"),
    });
  }
};

export const getByClassByPredmet = async (req, res) => {
  try {
    const ktp = await Ktp.find({
      ktpPredmet: req.params.predmetId,
      ktpClass: req.params.classId,
    }).exec();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("ktpGetPlanListFailed"),
    });
  }
};

export const getByMyClassByPredmet = async (req, res) => {
  try {
    const ktp = await Ktp.find({
      ktpPredmet: req.params.predmetId,
      ktpClass: req.params.classId,
    }).exec();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("ktpGetPlanListFailed"),
    });
  }
};

export const getAllKtp = async (req, res) => {
  try {
    const ktp = await Ktp.find().exec();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("ktpGetPlanListFailed"),
    });
  }
};

export const getPeriod = async (req, res) => {
  try {
    const ktp = await Ktp.find({
      ktpPredmet: req.params.predmetId,
      ktpPeriod: req.params.period,
      ktpClass: req.params.classId,
    }).exec();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("ktpGetPlanListFailed"),
    });
  }
};

export const deleteKtpMark = async (req, res) => {
  const ktpId = req.params.id;

  try {
    const ktp = await Ktp.findById(ktpId);

    if (!ktp) {
      return res.status(404).json({ message: i18n.__("ktpFindError") });
    }

    await Mark.deleteMany({ markDate: ktpId });
    await Ktp.deleteOne({ _id: ktpId });

    res.status(200).json({ message: i18n.__("ktpDeleteWithMarks") });
  } catch (err) {
    res.status(500).json({
      message: i18n.__("serverError"),
    });
  }
};
