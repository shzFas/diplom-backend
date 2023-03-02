import Ktp from '../models/Ktp.js';

export const createKtp = async (req, res) => {
  try {
    const doc = new Ktp({
      ktpTitle: req.body.ktpTitle,
      ktpDate: req.body.ktpDate,
      ktpPredmet: req.body.ktpPredmet,
      ktpClass: req.body.ktpClass,
      ktpTeacher: req.body.ktpTeacher,
      ktpSorSoch: req.body.ktpSorSoch,
    });
    const ktp = await doc.save();
    res.json(ktp);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось создать план',
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
        returnDocument: 'after',
      },
      (err, doc) => {
        if (err) {
          console.log(err);
          return res.status(500).json({
            message: 'Не удалось вернуть план',
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: 'Планы не найдены',
          });
        }

        res.json(doc);
      },
    )
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить список планов',
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
      message: 'Не удалось получить список планов',
    });
  }
};