@echo off
REM Wrapper : permet d'executer "ClientWriter <num> <texte>" depuis ce dossier
java -cp target\rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientWriter %*
