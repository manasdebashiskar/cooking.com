package com.cooking

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

class IngestionSpec extends FunSuite with DataFrameSuiteBase with BeforeAndAfterAll {
  val filePath = "./src/test/resources/test.csv"

  test("Correctly reading timestamp data using spark-csv") {
    implicit val sqlC = sqlContext
    val input = Ingestion.readInput(filePath)
    assert(input.count == 4)
    input.show()
  }
  ignore("Correctly writes data frame to Solr") {
    //TODO
  }
  ignore("Average number of recipes which are updated per hour") {
    //Eg. Pasta got updated twice in one hour
    
  }
  ignore("Number of recipes which got updated at 10:00 clock in the entire year") {
    
  }
}