import Ktp from "../models/Ktp.js";
import Mark from "../models/Mark.js";

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
        message: "Уже есть такой КТП",
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
          message: "Не возможно создать больше 1 СОЧ в четверти",
        });
        return next();
      }
    }
    const ktp = await doc.save();
    res.json(ktp);
  } catch (err) {
    res.status(500).json({
      message: "Не удалось создать план",
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
          console.log(err);
          return res.status(500).json({
            message: "Не удалось вернуть план",
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: "Планы не найдены",
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: "Не удалось получить список планов",
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
    console.log(err);
    res.status(500).json({
      message: "Не удалось получить список планов",
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
    console.log(err);
    res.status(500).json({
      message: "Не удалось получить список планов",
    });
  }
};

export const getAllKtp = async (req, res) => {
  try {
    const ktp = await Ktp.find().exec();
    res.json(ktp);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: "Не удалось получить список планов",
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
    console.log(err);
    res.status(500).json({
      message: "Не удалось получить список планов",
    });
  }
};

export const deleteKtpMark = async (req, res) => {
  const ktpId = req.params.id;

  try {
    const ktp = await Ktp.findById(ktpId);

    if (!ktp) {
      return res.status(404).json({ message: "КТП не найден" });
    }

    await Mark.deleteMany({ markDate: ktpId });
    await Ktp.deleteOne({ _id: ktpId });

    res.status(200).json({ message: "КТП и все оценки за этот урок удалены" });
  } catch (err) {
    res.status(500).json({
      message: "Ошибка сервера",
    });
  }
};
