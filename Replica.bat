@echo off
REM Wrapper : permet d'executer "Replica <id>" depuis ce dossier
REM Equivalent a : java -cp target\... com.tp.replication.Replica <id>
java -cp target\rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.Replica %*
