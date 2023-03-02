import mongoose from 'mongoose';

const PredmetSchema = new mongoose.Schema(
  {
    predmetName: {
      type: String,
      required: true,
      unique: true,
    },
    classes: {
      type: Array,
      required: true,
    }
  },
  {
    timestamps: true,
  },
);

export default mongoose.model('Predmet', PredmetSchema);
