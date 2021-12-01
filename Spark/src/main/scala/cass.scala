/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */
import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}

import java.util.UUID
import scala.collection.JavaConverters._

object cass {

  // This code uses the configuration present in ./resources/application.conf
  // and the AWS default credential chain.
  // see https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html.
  def main(args: Array[String]): Unit = {
    val resultSet = session.execute("select * from system_schema.keyspaces");
    val rows = resultSet.all().asScala;
    rows.foreach({ println } );

    println("List of all Keyspaces in this region...");
    for (row <- rows) println(row.getString("keyspace_name"));

    @transient val prepareStatement = session.prepare(
      s"""INSERT INTO log_gen_keyspace.log_data
         |   (log_id, file_name, log_message, log_type, timestamp) VALUES
         |   (?, ?, ?, ?, ?)""".stripMargin)
    @transient val parameterizedStatement = prepareStatement.bind(UUID.randomUUID(), "file2", "test2", "INFO", "11:11:11.0")
    @transient val statement = parameterizedStatement.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
    session.execute(statement)

    System.exit(0);
  }

  private val session = CqlSession.builder.build()
}