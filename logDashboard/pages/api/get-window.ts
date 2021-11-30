import { NextApiRequest, NextApiResponse } from 'next';
import mysql from 'mysql2/promise';

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  // create the connection

  const {query: {logLevel, windowSize}} = req

  const connection = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: 'Ciao123$',
    database: 'logsdb',
  });
  // query database
  const [rows, fields] = await connection.execute('SELECT COUNT(*) as NumberOfLogsInWindow FROM logs WHERE loglevel ="' + logLevel + '" GROUP BY UNIX_TIMESTAMP(time) DIV ' + windowSize);
  res.status(200).json(rows);
};
