import { NextApiRequest, NextApiResponse } from 'next';
//import mysql from 'mysql2/promise';
import cassandra from 'cassandra-driver';
import * as fs from 'fs';


const sigV4 = require('aws-sigv4-auth-cassandra-plugin');

const auth = new sigV4.SigV4AuthProvider({
  region: 'us-east-2', 
  accessKeyId:'AKIAQUBRNRF5RW5MJBBW',
  secretAccessKey: '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'});

  console.log("success")
  const sslOptions1 = {
    ca: [
        fs.readFileSync('/Users/andreacappelletti/Downloads/logDashboard/pages/api/sf-class2-root.crt', 'utf-8')],
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

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {

const query = 'SELECT * FROM log_gen_keyspace.log_data';
console.log("GENERATING RESULTS ....")
const result = (await client.execute(query)).rows;
console.log("results " + result[0])

  res.status(200).json(client.execute(query));
};
