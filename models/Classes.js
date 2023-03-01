import mongoose from 'mongoose';

const ClassesSchema = new mongoose.Schema(
  {
    className: {
      type: String,
      required: true,
      unique: true,
    },
  },
  {
    timestamps: true,
  },
);

export default mongoose.model('Classes', ClassesSchema);
