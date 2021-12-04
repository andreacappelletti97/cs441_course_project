exports.logDataFunction = (req, res) => {
    const cassandra = require('cassandra-driver');
    const fs = require('fs');
    const sigV4 = require('aws-sigv4-auth-cassandra-plugin');

    const auth = new sigV4.SigV4AuthProvider({
        region: 'us-east-2',
        accessKeyId: 'AKIAQUBRNRF5RW5MJBBW',
        secretAccessKey: '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'
    });

    const sslOptions1 = {
        ca: [
            fs.readFileSync('./cert/sf-class2-root.crt', 'utf-8')
        ],
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

    client.execute(query).then(
            result => res.send(result.rows.slice(0, 15)))
        .catch(e => res.send(e))

}