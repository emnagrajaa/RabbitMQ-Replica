@echo off
REM Simulation de la question 6 du TP
REM Assurez-vous que les 3 replicas sont deja lances (start_replicas.bat)

set JAR=target\rabbitmq-replication-1.0-SNAPSHOT.jar

echo === ETAPE 1: Ecriture de message1 et message2 ===
java -cp %JAR% com.tp.replication.ClientWriter 1 Texte message1
timeout /t 1 >nul
java -cp %JAR% com.tp.replication.ClientWriter 2 Texte message2
timeout /t 2 >nul

echo.
echo === ETAPE 2: Arretez manuellement Replica 2 maintenant ! ===
echo Appuyez sur une touche quand Replica 2 est arrete...
pause >nul

echo.
echo === ETAPE 3: Ecriture de message3 et message4 (Replica 2 en panne) ===
java -cp %JAR% com.tp.replication.ClientWriter 3 Texte message3
timeout /t 1 >nul
java -cp %JAR% com.tp.replication.ClientWriter 4 Texte message4
timeout /t 2 >nul

echo.
echo === ETAPE 4: Relancez Replica 2 maintenant ! ===
echo Appuyez sur une touche quand Replica 2 est relance...
pause >nul

echo.
echo === ETAPE 5: Verification avec ClientReaderV2 (vote majoritaire) ===
java -cp %JAR% com.tp.replication.ClientReaderV2

echo.
echo === Verifiez aussi les fichiers : ===
echo replica_1\data.txt
echo replica_2\data.txt
echo replica_3\data.txt
