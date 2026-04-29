@echo off
REM Wrapper : permet d'executer "ClientReader" depuis ce dossier
java -cp target\rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientReader %*
