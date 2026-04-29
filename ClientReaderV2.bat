@echo off
REM Wrapper : permet d'executer "ClientReaderV2" depuis ce dossier
java -cp target\rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientReaderV2 %*
