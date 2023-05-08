import mongoose from 'mongoose';

const StudentSchema = new mongoose.Schema(
  {
    fullName: {
      type: String,
      required: true,
    },
    email: {
      type: String,
      required: true,
      unique: true,
    },
    passwordHash: {
      type: String,
      required: true,
    },
    classId: {
      type: String,
      required: true,
    },
    telegram_id: {
      type: String,
      unique: true,
    },
    telegram_username: {
      type: String,
      unique: true,
    },
    avatarUrl: String,
  },
  {
    timestamps: true,
  },
);

export default mongoose.model('Students', StudentSchema);
