package SampleSpark
import SampleSpark.SparkRDD.readMovieItem
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{FloatType, IntegerType, StructType}
import org.apache.spark.sql.{Row, RowFactory, SparkSession}

import java.nio.charset.CodingErrorAction
import scala.io.Codec

class SparkSQL

object SparkSQL {
  implicit val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "/")

    val spark = SparkSession.builder().appName("SparkSQL").master("local[1]").getOrCreate()

    val mapMovie = readMovieItem()

    // Read data
    val lines = spark.sparkContext.textFile("./log/u.data")

    // Create Schema
    val schema = new StructType()
      .add("movieID", IntegerType)
      .add("rating", FloatType)

    // Create Rows and create dataFrame
    val movies: RDD[Row] = lines.map(line => {
      val fields = line.split("\\s+")
      Row(fields(1).toInt, fields(2).toFloat)
    })
    val movieDataset = spark.createDataFrame(movies, schema)

    val avgRating = movieDataset.groupBy("movieID").avg("rating")

    val counts = movieDataset.groupBy("movieID").count()

    val avgCount = counts.join(avgRating, "movieID")

    val topTen = avgCount.orderBy("avg(rating)").take(10)

    topTen.foreach(result => {
      println(s"Something ${mapMovie(result.getInt(0))} and ${result.get(1)} and ${result.get(2)}")
    })
  }
}
