import mongoose from 'mongoose';

const MarkSchema = new mongoose.Schema(
  {
    markTeacher: {
      type: String,
      required: true,
    },
    markPredmet: {
      type: String,
      required: true,
    },
    markStudent: {
      type: String,
      required: true,
    },
    markClassStudent: {
      type: String,
      required: true,
    },
    markDate: {
      type: String,
      required: true,
    },
    markFalse: {
      type: Boolean,
      required: true,
    },
    markMaxValue: {
      type: Number,
      required: true,
    },
    markValue: {
      type: Number,
      required: true,
    }
  },
  {
    timestamps: true,
  },
);

export default mongoose.model('Mark', MarkSchema);
