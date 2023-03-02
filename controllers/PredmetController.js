import Predmet from '../models/Predmet.js';

export const createPredmet = async (req, res) => {
  try {
    const doc = new Predmet({
      predmetName: req.body.predmetName,
      classes: req.body.classes,
    });
    const predmet = await doc.save();
    res.json(predmet);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось создать предмет',
    });
  }
};

export const getOne = async (req, res) => {
  try {
    const predmet = req.params.id;

    Predmet.findOneAndUpdate(
      {
        _id: predmet,
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
            message: 'Не удалось вернуть предмет',
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: 'Предметы не найдены',
          });
        }

        res.json(doc);
      },
    )
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить список предмета',
    });
  }
};

export const getAllPredmets = async (req, res) => {
  try {
    const predmet = await Predmet.find().exec();
    res.json(predmet);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить список предметов',
    });
  }
};