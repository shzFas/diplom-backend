import ClassModel from '../models/Classes.js';

export const createClasslist = async (req, res) => {
  try {
    const doc = new ClassModel({
      className: req.body.className,
    });
    const classes = await doc.save();
    res.json(classes);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось создать класс',
    });
  }
};

export const getOne = async (req, res) => {
  try {
    const classes = req.params.id;

    ClassModel.findOneAndUpdate(
      {
        _id: classes,
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
            message: 'Не удалось вернуть класс',
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: 'Класс не найден',
          });
        }

        res.json(doc);
      },
    )
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить список класса',
    });
  }
};

export const getAllClasses = async (req, res) => {
  try {
    const classes = await ClassModel.find().exec();
    res.json(classes);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить классы',
    });
  }
};