import com.typesafe.sbt.packager.chmod
import sbt._

object scriptGenerator {
  private val startScript =
    """#!/bin/bash

app_dir=$(readlink -f "$(dirname "$(readlink -f "$0")")"/..)

NAME=$(basename $app_dir)

if [ -f "$app_dir/bin/settings.sh" ]; then
  . $app_dir/bin/settings.sh
else
  echo "Missing $app_dir/bin/settings.sh, exiting"
  exit 1
fi
FILEPATH=$1
cmd="spark-submit \
  --master yarn \
  --deploy-mode cluster \
  --name ${NAME}_${FILE} \
  --class %s \
  --files $app_dir/conf/log4j.properties,$app_dir/conf/application.conf,$app_dir/conf/metrics.properties \
  --conf spark.metrics.conf=metrics.properties \
  --conf spark.yarn.submit.waitAppCompletion=true \
  --conf spark.dynamicAllocation.maxExecutors=$DYNAMICALLOCATION_MAXEXECUTORS \
  --conf spark.hadoop.validateOutputSpecs=false \
  --driver-memory $DRIVER_MEM --driver-class-path ./ \
  --executor-memory $EXECUTOR_MEM --conf spark.executor.extraClassPath=./ \
  $app_dir/lib/%s --file \"$FILEPATH\"


#echo "Starting ingestion job...."
#app_id=$(eval $cmd |& grep "tracking URL" | awk '{print $3}' | awk -F '/' '{print $5}')
#echo $app_id > $app_dir/app_id
#echo "ingestion job started, YARN app id is: $app_id."
echo $cmd
eval $cmd
"""

  private val stopScript =
    """#!/bin/bash
app_dir=$(readlink -f "$(dirname "$(readlink -f "$0")")"/..)
app_id_path=$app_dir/app_id

if [ ! -f $app_id_path ]; then
  echo 'Could not find app id'
else
  app_id=$(cat $app_id_path)
  yarn application -kill $app_id && rm $app_id_path
fi
"""

  private val settingsScript =
    """
    EXECUTOR_MEM=1G
    DRIVER_MEM=1G
    DYNAMICALLOCATION_MAXEXECUTORS=3
    """
  private val restartScript =
    """#!/bin/bash

    app_dir=$(readlink -f "$(dirname "$(readlink -f "$0")")"/..)
    app_id_path=$app_dir/app_id
    
    if [ ! -f $app_id_path ]; then
       source $app_dir/bin/start_Ingestion.sh
    else
      app_id=$(cat $app_id_path)
      app_status=$(yarn application -status $app_id 2> /dev/null | grep [[:space:]]State | awk '{print $3}')
      echo $app_status
      if [ -z $app_id ]; then
        source $app_dir/bin/start_Ingestion.sh
      elif [[ $app_status == "FINISHED" || $app_status == "FAILED" || $app_status == "KILLED" ]]; then
           dt=`date +%s`
           app_id_newpath=${app_id}_${dt}
           cp $app_id_path $app_id_newpath
           source $app_dir/bin/start_Ingestion.sh
      fi
    fi
    """
  val startScriptName = "start_Ingestion.sh"
  val stopScriptName = "stop_Ingestion.sh"
  val settingsScriptName = "settings.sh"
  val restartScriptName = "restart_Ingestion.sh"

  def createStartScriptResources(base: File, jarName: String, mainClassName: String): Seq[File] = {
    val startScriptFile = base / startScriptName
    val startScriptContents = startScript.format(mainClassName, jarName)
    IO.write(startScriptFile, startScriptContents)
    chmod(startScriptFile, "755")
    val stopScriptFile = base / stopScriptName
    IO.write(stopScriptFile, stopScript)
    chmod(stopScriptFile, "755")
    val settingsFile = base / settingsScriptName
    IO.write(settingsFile, settingsScript)
	  chmod(settingsFile, "755")
    val restartScriptFile = base / restartScriptName
    IO.write(restartScriptFile, restartScript)
    chmod(restartScriptFile, "755")
    Seq(startScriptFile, stopScriptFile, settingsFile,restartScriptFile)
  }

}
