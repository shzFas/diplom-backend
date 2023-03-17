import mongoose from 'mongoose';

const KtpSchema = new mongoose.Schema(
  {
    ktpTitle: {
      type: String,
      required: true,
    },
    ktpDate: {
      type: String,
      required: true,
    },
    ktpPredmet: {
      type: String,
      required: true,
    },
    ktpClass: {
      type: String,
      required: true,
    },
    ktpTeacher: {
      type: String,
      required: true,
    },
    ktpSorSoch: {
      type: String,
      required: true,
    },
    ktpMaxValue: {
      type: Number,
      required: true,
    },
    ktpPeriod: {
      type: String,
      required: true,
    },
  },
  {
    timestamps: true,
  },
);

export default mongoose.model('Ktp', KtpSchema);
