@echo off
REM Lance les 3 replicas dans des fenetres separees
REM Equivalent aux commandes : Replica 1 / Replica 2 / Replica 3

start "Replica 1" cmd /k Replica.bat 1
timeout /t 1 >nul
start "Replica 2" cmd /k Replica.bat 2
timeout /t 1 >nul
start "Replica 3" cmd /k Replica.bat 3

echo Les 3 replicas sont lances.
