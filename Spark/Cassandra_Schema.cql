CREATE KEYSPACE IF NOT EXISTS log_gen_keyspace
    WITH REPLICATION = {
        'class': 'SimpleStrategy',
        'replication_factor': '1'
    };

USE log_gen_keyspace;

CREATE TABLE log_gen_keyspace.log_data (log_id uuid PRIMARY KEY, timestamp text, log_type text, log_message text);