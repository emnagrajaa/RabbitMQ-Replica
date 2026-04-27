# TP RabbitMQ - Réplication de Données

## Architecture

```
ClientWriter ──► write_exchange (fanout) ──► replica_queue_1 ──► Replica 1 ──► replica_1/data.txt
                                         ──► replica_queue_2 ──► Replica 2 ──► replica_2/data.txt
                                         ──► replica_queue_3 ──► Replica 3 ──► replica_3/data.txt

ClientReader ──► read_exchange (fanout)  ──► replica_queue_1 ──► Replica 1 ──┐
                                         ──► replica_queue_2 ──► Replica 2 ──┤──► replyQueue ──► ClientReader
                                         ──► replica_queue_3 ──► Replica 3 ──┘   (1ère réponse)
```

## Prérequis

- Java 11+
- Maven 3.6+
- RabbitMQ installé et démarré sur `localhost:5672`

## Compilation

```bash
cd RabbitMQ
mvn clean package -q
```

Le JAR produit : `target/rabbitmq-replication-1.0-SNAPSHOT.jar`

---

## Lancement

### 1. Démarrer RabbitMQ
```bash
rabbitmq-server   # ou via le service Windows
```

### 2. Démarrer les 3 Replicas (terminal séparés)
```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.Replica 1
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.Replica 2
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.Replica 3
```
Ou utiliser : `start_replicas.bat`

---

## Questions du TP

### Q1-3 : ClientWriter → écriture sur tous les replicas
```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientWriter 1 Texte message1
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientWriter 2 Texte message2
```
→ Vérifier `replica_1/data.txt`, `replica_2/data.txt`, `replica_3/data.txt`

### Q4-5 : ClientReader → lecture de la dernière ligne
```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientReader
```
→ Retourne la **première réponse** reçue (haute disponibilité).  
→ Arrêter Replica 2, relancer ClientReader → fonctionne toujours grâce aux replicas 1 et 3.

### Q6 : Simulation de panne (incohérence des données)
Utiliser `demo_q6.bat` ou manuellement :
1. Écrire lignes 1 et 2 avec ClientWriter
2. **Arrêter Replica 2**
3. Écrire lignes 3 et 4 avec ClientWriter
4. **Relancer Replica 2**
5. Comparer les 3 fichiers → Replica 2 manque les lignes 3 et 4

> **Explication** : Les queues sont `auto-delete`. Quand Replica 2 s'arrête,  
> sa queue disparaît et les messages envoyés pendant la panne sont perdus.

### Q7 : ClientReaderV2 → vote majoritaire
```bash
java -cp target/rabbitmq-replication-1.0-SNAPSHOT.jar com.tp.replication.ClientReaderV2
```
→ Envoie `READ_ALL` à tous les replicas  
→ Chaque replica répond ligne par ligne puis envoie `END`  
→ Une ligne est affichée si présente dans **≥ 2 replicas sur 3**  
→ Résout l'incohérence de la question 6

---

## Format du fichier texte
```
1 Texte message1
2 Texte message2
3 Texte message3
4 Texte message4
```

## Classes Java

| Classe | Rôle |
|---|---|
| `RabbitMQConfig` | Constantes partagées (host, exchanges, queues) |
| `Replica` | Processus replica (WRITE / READ_LAST / READ_ALL) |
| `ClientWriter` | Émet une ligne vers tous les replicas |
| `ClientReader` | Lit la dernière ligne (1ère réponse) |
| `ClientReaderV2` | Lit tout avec vote majoritaire |
