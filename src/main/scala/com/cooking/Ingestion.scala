package com.cooking

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.log4j.Logger
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types._
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import org.apache.spark.sql.DataFrame
import scala.collection.JavaConverters._
import org.apache.spark.sql.SaveMode

object Ingestion {
  val logger = Logger.getLogger(getClass)
  def main(args: Array[String]) = {
    val path = args(0)
    val config = ConfigFactory.load.getConfig("com.cooking.ingestion")
    val conf = new SparkConf().setAppName(getClass.toString).setMaster("local[*]") //TODO: Get other settings from configuration. and do setAll
    val sparkContext = new SparkContext(conf)
    //Create the SQLContext
    implicit val sqlContext = new SQLContext(sparkContext)
    //Read the input CSV to a DataFrame
    val input = readInput(path)
    //Write the Dataframe to Solr.
    writeToSolr(input, config)
  }

  /**
   * This function reads a CSV and converts to a dataframe.
   * @param path:String it is the file path in HDFS.
   * @returns Dataframe
   */
  def readInput(path: String)(implicit sqlContext: SQLContext): DataFrame = {

    val schema: StructType = StructType(
      StructField("recipe_id", LongType, nullable = false) ::
        StructField("recipe_name", StringType, nullable = false) ::
        StructField("description", StringType, nullable = true) ::
        StructField("ingredient", StringType, nullable = false) ::
        StructField("active", StringType, nullable = false) ::
        StructField("updated_date", TimestampType, nullable = false) ::
        StructField("created_date", TimestampType, nullable = false) :: Nil)

    sqlContext.read
      .format("com.databricks.spark.csv")
      .option("parserLib", "univocity")
      .option("delimiter", ",")
      .option("nullValue", "null")
      .option("ignoreLeadingWhiteSpace", "true")
      .option("ignoreTrailingWhiteSpace", "true")
      .option("header", "true")
      .schema(schema)
      .load(path)
  }

  /**
   * This function writes the data frame to solr based on the configuration.
   * @param input: DataFrame is the entity pushed to Solr
   * @param config: Config is the configuration that provides solr connection.
   * @return Unit
   * Assumption the collection is created and schema is provided.
   * Alternatively we can have ManagedIndexSchemaFactory enabled.
   */
  def writeToSolr(input: DataFrame, config: Config) = {
    val solrConfig = config.getConfig("solr").entrySet().asScala.map { s => (s.getKey, s.getValue.unwrapped.toString()) }.toMap
    // Write to Solr
    input.write.format("solr").options(solrConfig).mode(SaveMode.Overwrite).save

  }

  /**
   * This function shall send stats for each recipe to openTSDB.
   * By sending the stats to openTSDB and doing rollup and pre-aggregation
   * we can get answers to queries like number of updates over hours etc.
   * @param input: DataFrame is the input whose stats are sent out
   * @param: config: Config provides config for openTSDB connection
   * @return Unit
   */
  def sendStats(input:DataFrame, config: Config) = ???
}