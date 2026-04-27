@echo off
REM Lance les 3 replicas dans des fenetres separees
set JAR=target\rabbitmq-replication-1.0-SNAPSHOT.jar
REM JAR contient toutes les dependances (fat jar via maven-shade-plugin)

start "Replica 1" cmd /k java -cp %JAR% com.tp.replication.Replica 1
timeout /t 1 >nul
start "Replica 2" cmd /k java -cp %JAR% com.tp.replication.Replica 2
timeout /t 1 >nul
start "Replica 3" cmd /k java -cp %JAR% com.tp.replication.Replica 3

echo Les 3 replicas sont lances.
