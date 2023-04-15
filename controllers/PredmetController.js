import i18n from "i18n";
import Predmet from "../models/Predmet.js";

export const createPredmet = async (req, res) => {
  try {
    const doc = new Predmet({
      predmetName: req.body.predmetName,
      classes: req.body.classes,
    });
    await doc.save();
    res.status(200).json({
      message: i18n.__("subjectCreateSuccess"),
    });
  } catch (err) {
    res.status(500).json({
      message: i18n.__("subjectCreateFailed"),
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
        returnDocument: "after",
      },
      (err, doc) => {
        if (err) {
          return res.status(500).json({
            message: i18n.__("subjectReturnFailed"),
          });
        }

        if (!doc) {
          return res.status(404).json({
            message: i18n.__("subjectsNotFound"),
          });
        }

        res.json(doc);
      }
    );
  } catch (err) {
    res.status(500).json({
      message: i18n.__("subjectListGetFailed"),
    });
  }
};

export const getAllPredmets = async (req, res) => {
  try {
    const predmet = await Predmet.find().exec();
    res.json(predmet);
  } catch (err) {
    res.status(500).json({
      message: i18n.__("subjectAllListGetFailed"),
    });
  }
};

export const addClasses = async (req, res) => {
  const { classes } = req.body;
  const id = req.params.predmetId;

  try {
    const predmet = await Predmet.findById(id);

    if (!predmet) {
      return res.status(404).json({ message: i18n.__("subjectNotFound") });
    }

    const newClasses = classes.filter(
      (c) => !predmet.classes.some((pc) => pc._id === c._id)
    );

    if (newClasses.length === 0) {
      return res.json({ message: i18n.__("subjectNewClassesNotAdded") });
    }

    predmet.classes.push(...newClasses);
    await predmet.save();

    return res.json({ message: i18n.__("subjectClassesSuccessfullyAdded") });
  } catch (error) {
    return res.status(500).json({ message: i18n.__("serverError") });
  }
};

export const deleteClassPredmet = async (req, res) => {
  const { predmetId, classId } = req.params;

  try {
    const predmet = await Predmet.findById(predmetId);

    if (!predmet) {
      return res.status(404).json({ message: i18n.__("classFindError") });
    }
    const classIndex = predmet.classes.findIndex((data) => data._id == classId);

    if (classIndex === -1) {
      return res.status(404).json({ error: i18n.__("classFindError") });
    }

    predmet.classes.splice(classIndex, 1);
    await predmet.save();

    res.status(200).json({ message: i18n.__("subjectClassDeleted") });
  } catch (error) {
    return res.status(500).json({ message: i18n.__("serverError") });
  }
};

export const deletePredmetById = async (req, res) => {
  const predmetId = req.params.predmetId;

  try {
    const deletedPredmet = await Predmet.findByIdAndDelete(predmetId);

    if (!deletedPredmet) {
      return res.status(404).json({ message: i18n.__("subjectNotFound") });
    }

    res.status(200).json({ message: i18n.__("subjectDeletedSuccessfully") });
  } catch (error) {
    res.status(500).json({ message: i18n.__("subjectDeletionFailed"), error });
  }
};

export const getPredmetByClass = async (req, res) => {
  const classId = req.params.id;

  try {
    const predmets = await Predmet.find({ "classes._id": classId });
    res.status(200).json(predmets);
  } catch (err) {
    res.status(500).json({ message: i18n.__("serverError") });
  }
};
