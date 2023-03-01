import express from 'express';
import cors from 'cors';

import mongoose from 'mongoose';

mongoose
  .connect(`ССЫЛКА НА МОНГУ`)
  .then(() => console.log('DB ok'))
  .catch((err) => console.log('DB error', err));

const app = express();
app.use(express.json());
app.use(cors());

app.listen(process.env.PORT || 4444, (err) => {
  if (err) {
    return console.log(err);
  }
  console.log('Server OK');
});
