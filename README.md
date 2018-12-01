# README

This program loads CSV file to  `solr` collection while sending stats of records to `openTSDB`.

## Compile

This is a sbt based program so it needs sbt *(tested with 0.13.16 and higher)*.

The command to compile the program is `sbt compile`

## Test

The command to test the program is `sbt test`

## Assembly

The command to create an assembly is `sbt assembly`

## Universal Package

This project is designed to produce an `zip` package from the assembly structure.

The command to create an `zip` is by running `sbt universal:packageBin` command.
The desired `zip` is then placed at `target/universal/` directory.

## Deployment

The deployment structure of the package looks similar to the example below.

    /home/cloudera/cooking.com
    ├── bin
    │   ├── restart_Ingestion.sh
    │   ├── settings.sh
    │   ├── start_Ingestion.sh
    │   └── stop_Ingestion.sh
    ├── conf
    │   ├── application.conf
    │   ├── log4j.properties
    │   └── metrics.properties
    └── lib
        └── Ingestion1-assembly-1.0.0.jar

