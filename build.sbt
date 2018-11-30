import scriptGenerator._

lazy val root = (project in file(".")).
  settings(inThisBuild(List(
    organization := "com.cooking",
    scalaVersion  := "2.10.7"
  ))).
  settings(
    name := "cooking.com",
    version := "0.1",
    fork in Test := true,
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
    parallelExecution in Test := true,
    resolvers ++=
      Seq(
        "maven" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
        "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      ),

    libraryDependencies ++=
      Seq(
        "org.apache.spark" %% "spark-core" % "1.6.3",
        "org.apache.spark" %% "spark-sql" % "1.6.3",
        "com.databricks" %% "spark-csv" % "1.5.0",
        "com.github.scopt" %% "scopt" % "3.7.0",
        "com.holdenkarau" %% "spark-testing-base" % "1.6.0_0.9.0" % "test",
        "org.scalatest" %%   "scalatest" % "2.2.4" % "test",
        "com.lucidworks.spark" % "spark-solr" % "2.4.0",
        "com.cgnal.spark" %% "spark-opentsdb" % "1.0"
      )
  )

publishTo := Some("Internal Publish Repository" at "http://mvn-repo.dev.cooking.com/artifactory/local-release/")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

enablePlugins(UniversalPlugin)

  // removes all jar mappings in universal and appends the fat jar from assembly
mappings in Universal := {
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  // filter out all jars
  val filtered = universalMappings filter {
    case (file, name) =>  ! name.endsWith(".jar")
  }
  // add the fat jar
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

// add config file, log4j, and submit script to mappings
mappings in Universal ++= {
  val conf = (resourceDirectory in Compile).value / "application.conf"
  val log4j = (resourceDirectory in Compile).value / "log4j.properties"
  val startScript = (resourceManaged in Compile).value / startScriptName
  val restartScript = (resourceManaged in Compile).value / restartScriptName
  val stopScript = (resourceManaged in Compile).value / stopScriptName
  val settings = (resourceManaged in Compile).value / settingsScriptName
  val metrics = (resourceDirectory in Compile).value / "metrics.properties"

  Seq(conf -> "conf/application.conf",
    log4j -> "conf/log4j.properties",
    metrics -> "conf/metrics.properties",
    startScript -> s"bin/$startScriptName",
    stopScript -> s"bin/$stopScriptName",
    restartScript -> s"bin/$restartScriptName",
    startScript -> s"bin/$startScriptName",
    stopScript -> s"bin/$stopScriptName",
    restartScript -> s"bin/$restartScriptName",
    settings -> s"bin/$settingsScriptName"
    )
}
// Generate start script
resourceGenerators in Compile += Def.task {
  createStartScriptResources((resourceManaged in Compile).value, (assemblyJarName in assembly).value, (mainClass in Compile).value.get)
}.taskValue
