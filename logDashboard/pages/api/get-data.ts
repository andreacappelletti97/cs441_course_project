import { NextApiRequest, NextApiResponse } from 'next';
//import mysql from 'mysql2/promise';
const cassandra = require('cassandra-driver');
const fs = require('fs');
const sigV4 = require('aws-sigv4-auth-cassandra-plugin');

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {

  const auth = new sigV4.SigV4AuthProvider({
    region: 'us-east-2', 
    accessKeyId:'AKIAQUBRNRF5RW5MJBBW',
    secretAccessKey: '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'});
  
    const sslOptions1 = {
      ca: [
          fs.readFileSync('sf-class2-root.crt', 'utf-8')],
      host: 'cassandra.us-east-2.amazonaws.com',
      rejectUnauthorized: true
    };
const client = new cassandra.Client({
  contactPoints: ['cassandra.us-east-2.amazonaws.com'],
  localDataCenter: 'us-east-2',
  authProvider: auth,
  sslOptions: sslOptions1,
  protocolOptions: { port: 9142 }
});

const query = 'SELECT * FROM log_gen_keyspace.log_data';
  const [rows] = await client.execute(query);
  console
  res.status(200).json(rows);
};
