package Cassandra

import org.apache.spark.{SparkConf, SparkContext}

class CassandraSetup

object CassandraSetup {
  val cassandraHost = "127.0.0.1"
  val cassandraPort = "9042"
  val keyspace = "log_gen_keyspace"
  val table = "log_data"

  def apply(conf: SparkConf) : Unit = {
    conf.set("spark.cassandra.connection.host", cassandraHost)
    conf.set("spark.cassandra.connection.port", cassandraPort)
  }
}

