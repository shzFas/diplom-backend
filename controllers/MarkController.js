import Mark from '../models/Mark.js';

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
    const ktp = await doc.save();
    res.json(ktp);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось поставить оценку',
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
        returnDocument: 'after',
      },
      (err, doc) => {
        if (err) {
          console.log(err);
          return res.status(500).json({
            message: 'Не удалось вернуть оценку',
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: 'Оценка не найдена',
          });
        }

        res.json(doc);
      },
    )
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить оценку',
    });
  }
};

export const getByStudentByKtp = async (req, res) => {
  try {
    const ktp = await Mark.find(
      {
        markStudent: req.params.studentId,
        markDate: req.params.ktpId,
      },
    ).exec();
    if(ktp.length >= 1) {
      res.json(ktp);
    }
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить Оценку',
    });
  }
};

export const getByStudentByPredmet = async (req, res) => {
  try {
    const ktp = await Mark.find(
      {
        markStudent: req.params.studentId,
        markPredmet: req.params.predmetId,
        markSochSor: req.params.type,
        markPeriod: req.params.period,
      },
    ).exec();
    res.json(ktp);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить Оценки',
    });
  }
};

export const getAllMarks = async (req, res) => {
  try {
    const mark = await Mark.find().exec();
    res.json(mark);
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить список оценок',
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
          console.log(err);
          return res.status(500).json({
            message: 'Не удалось удалить оценку',
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: 'Оценка не найдена',
          });
        }

        res.json({
          success: true,
        });
      },
    );
  } catch (err) {
    console.log(err);
    res.status(500).json({
      message: 'Не удалось получить оценку',
    });
  }
};