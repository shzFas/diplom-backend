import ClassModel from "../models/Classes.js";
import i18n from "i18n";

export const createClasslist = async (req, res) => {
  try {
    const doc = new ClassModel({
      className: req.body.className,
    });
    await doc.save();
    res.status(200).json({
      message: i18n.__("classCreateSuccess"),
    });
  } catch (err) {
    res.status(500).json({
      message: i18n.__("classCreateError"),
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
        returnDocument: "after",
      },
      (err, doc) => {
        if (err) {
          return res.status(500).json({
            message: i18n.__("classReturnError"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("classFindError"),
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("classReturnStudentsError"),
    });
  }
};

export const getAllClasses = async (req, res) => {
  try {
    const classes = await ClassModel.find().exec();
    res.json(classes);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("classAllReturnError"),
    });
  }
};
