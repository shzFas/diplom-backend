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

export const addClasses = async (req, res) => {
  const { classes } = req.body;
  const id = req.params.predmetId;

  try {
    const predmet = await Predmet.findById(id);
    
    if (!predmet) {
      return res.status(404).json({ message: 'Предмет не найден' });
    }

    const newClasses = classes.filter((c) => !predmet.classes.some((pc) => pc._id === c._id));

    if (newClasses.length === 0) {
      return res.json({ message: 'Новые классы не добавлены' });
    }

    predmet.classes.push(...newClasses);
    await predmet.save();

    return res.json({ message: 'Классы успешно добавлены' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: 'Ошибка сервера' });
  }
};

export const deleteClassPredmet = async (req, res) => {
  const { predmetId, classId } = req.params;

  try {
    const predmet = await Predmet.findById(predmetId);

    if (!predmet) {
      return res.status(404).json({ message: 'Класс не найден' });
    }
    const classIndex = predmet.classes.findIndex(data => data._id == classId);

    if (classIndex === -1) {
      return res.status(404).json({ error: 'Класс не найден' });
    }

    predmet.classes.splice(classIndex, 1);
    await predmet.save();

    res.status(200).json({ message: 'Класс удален' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: 'Ошибка сервера' });
  }
}
