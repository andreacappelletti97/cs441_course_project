package SampleSpark

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import java.nio.charset.CodingErrorAction
import scala.collection.mutable.Map
import scala.io.Codec

class SparkRDD

object SparkRDD {
  implicit val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
  // For Logging Purposes
  // Mutable function
  def readMovieItem(): Map[Int, String] = {
    val movieMap = Map[Int, String]()
    val source = scala.io.Source.fromFile("./log/u.item")
    val lines = try source.mkString finally source.close()
    lines.split("\n").foreach(line => {
      if (line.trim().nonEmpty) {
        val fields = line.split("\\|")
        movieMap(fields(0).toInt) = fields(1)
      }
    })
    movieMap
  }

  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "/")

    val conf = new SparkConf().setAppName("fun").setMaster("local[*]")
    val sc = new SparkContext(conf)

    val immutableMovieIdMap = readMovieItem()

    val lines = sc.textFile("./log/u.data")

    // (movieID, (rating, 1.0))
    val movieRatings: RDD[(Int, (Float, Float))] = lines.map(line => {
      val fields = line.split("\\s+")
      (fields(1).toInt, (fields(2).toFloat, 1.0.toFloat))
    })

    // (movieID, (sumOfRating, totalRating))
    val ratingTotalAndCount: RDD[(Int, (Float, Float))] = movieRatings.reduceByKey((movie1, movie2) => {
      (movie1._1 + movie2._1, movie1._2 + movie2._2)
    })

    // Map To (MovieID, avg Rating)
    val avgRating: RDD[(Int, Float)] = ratingTotalAndCount.mapValues(totalAndCount => {
      totalAndCount._1 / totalAndCount._2
    })

    // Sorting by avg rating
    val sortedMovie = avgRating.sortBy(m => m._2)

    // take top 10 result
    val results = sortedMovie.take(10)

    results.foreach(result => {
      println(s"Something ${immutableMovieIdMap(result._1)} and ${result._2}")
    })
  }
}
