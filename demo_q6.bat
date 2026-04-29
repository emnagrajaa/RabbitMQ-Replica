@echo off
REM Simulation de la question 6 du TP
REM Assurez-vous que les 3 replicas sont deja lances (start_replicas.bat)

echo === ETAPE 1: Ecriture de message1 et message2 ===
ClientWriter.bat 1 Texte message1
timeout /t 1 >nul
ClientWriter.bat 2 Texte message2
timeout /t 2 >nul

echo.
echo === ETAPE 2: Arretez manuellement Replica 2 maintenant ! ===
echo (Fermez la fenetre "Replica 2" ou faites Ctrl+C dedans)
echo Appuyez sur une touche quand Replica 2 est arrete...
pause >nul

echo.
echo === ETAPE 3: Ecriture de message3 et message4 (Replica 2 en panne) ===
ClientWriter.bat 3 Texte message3
timeout /t 1 >nul
ClientWriter.bat 4 Texte message4
timeout /t 2 >nul

echo.
echo === ETAPE 4: Relancez Replica 2 maintenant ! ===
echo (Ouvrez un nouveau terminal et tapez : Replica 2)
echo Appuyez sur une touche quand Replica 2 est relance...
pause >nul

echo.
echo === ETAPE 5: Verification avec ClientReaderV2 (vote majoritaire) ===
ClientReaderV2.bat

echo.
echo === Verifiez aussi les fichiers : ===
echo.
type replica_1\data.txt 2>nul && echo [replica_1\data.txt affiche] || echo replica_1\data.txt introuvable
echo ---
type replica_2\data.txt 2>nul && echo [replica_2\data.txt affiche] || echo replica_2\data.txt introuvable
echo ---
type replica_3\data.txt 2>nul && echo [replica_3\data.txt affiche] || echo replica_3\data.txt introuvable
