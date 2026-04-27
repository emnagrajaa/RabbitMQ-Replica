package com.tp.replication;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Processus ClientReaderV2 (question 7).
 * Envoie "READ_ALL" à tous les replicas et collecte toutes leurs lignes.
 * Applique un vote majoritaire : une ligne est affichée si elle est présente
 * dans au moins 2 replicas sur 3.
 *
 * Protocole de réponse du Replica :
 *   "LINE:<contenu de la ligne>"  — pour chaque ligne du fichier
 *   "END"                         — fin de transmission pour ce replica
 *
 * Usage : java -cp <jar> com.tp.replication.ClientReaderV2
 */
public class ClientReaderV2 {

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQConfig.HOST);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(RabbitMQConfig.READ_EXCHANGE, BuiltinExchangeType.FANOUT);

        // File de réponse exclusive et temporaire
        String replyQueue = channel.queueDeclare().getQueue();
        String correlationId = UUID.randomUUID().toString();

        // Résultats par replica : replicaIndex → liste de lignes
        // On regroupe les messages entre chaque "END"
        List<List<String>> replicaLines = new ArrayList<>();
        for (int i = 0; i < RabbitMQConfig.REPLICA_COUNT; i++) {
            replicaLines.add(new ArrayList<>());
        }

        // File bloquante pour gérer les messages de manière séquentielle
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        channel.basicConsume(replyQueue, true,
            (consumerTag, delivery) -> {
                if (correlationId.equals(delivery.getProperties().getCorrelationId())) {
                    messageQueue.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                }
            },
            consumerTag -> {}
        );

        // Envoyer READ_ALL à tous les replicas
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .replyTo(replyQueue)
                .correlationId(correlationId)
                .build();

        channel.basicPublish(RabbitMQConfig.READ_EXCHANGE, "", props,
                "READ_ALL".getBytes(StandardCharsets.UTF_8));

        System.out.println("[ClientReaderV2] >> Requete READ_ALL envoyee a tous les replicas.");
        System.out.println("[ClientReaderV2] Collecte des reponses (timeout: "
                + RabbitMQConfig.REPLY_TIMEOUT_MS + " ms par message)...");

        // Collecter les messages jusqu'à recevoir REPLICA_COUNT "END"
        int endCount = 0;
        int replicaIndex = 0;

        while (endCount < RabbitMQConfig.REPLICA_COUNT) {
            String msg = messageQueue.poll(RabbitMQConfig.REPLY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (msg == null) {
                // Timeout : les replicas restants ne répondent pas (en panne)
                System.out.println("[ClientReaderV2] Timeout - " +
                        (RabbitMQConfig.REPLICA_COUNT - endCount) + " replica(s) ne repondent pas.");
                break;
            }

            if (msg.equals("END")) {
                endCount++;
                System.out.println("[ClientReaderV2] Replica " + (endCount) + " a termine.");
                replicaIndex++;
            } else if (msg.startsWith("LINE:") && replicaIndex < RabbitMQConfig.REPLICA_COUNT) {
                String line = msg.substring(5);
                replicaLines.get(replicaIndex).add(line);
            }
        }

        // ---- VOTE MAJORITAIRE ----
        // Compter combien de replicas possèdent chaque ligne unique
        Map<String, Integer> lineCount = new LinkedHashMap<>();

        for (List<String> lines : replicaLines) {
            // Chaque replica contribue max 1 vote par ligne (ensemble pour dédupliquer)
            Set<String> uniqueLinesForReplica = new LinkedHashSet<>(lines);
            for (String line : uniqueLinesForReplica) {
                lineCount.merge(line, 1, Integer::sum);
            }
        }

        // Trier les lignes par leur numéro de ligne (1er token numérique)
        List<String> majority = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : lineCount.entrySet()) {
            if (entry.getValue() >= 2) { // présente dans au moins 2/3 replicas
                majority.add(entry.getKey());
            }
        }

        // Trier par numéro de ligne
        majority.sort(Comparator.comparingInt(ClientReaderV2::extractLineNumber));

        // ---- AFFICHAGE ----
        System.out.println();
        System.out.println("+==================================================+");
        System.out.println("|         RESULTAT - VOTE MAJORITAIRE              |");
        System.out.println("+==================================================+");
        System.out.printf("| Replicas ayant repondu : %d / %d                    |%n",
                endCount, RabbitMQConfig.REPLICA_COUNT);
        System.out.println("+--------------------------------------------------+");

        System.out.println("| Contenu par replica :                            |");
        for (int i = 0; i < replicaLines.size(); i++) {
            System.out.println("|   Replica " + (i + 1) + " (" + replicaLines.get(i).size() + " lignes) : "
                    + replicaLines.get(i));
        }
        System.out.println("+--------------------------------------------------+");

        if (majority.isEmpty()) {
            System.out.println("| Aucune ligne ne satisfait le vote majoritaire.   |");
        } else {
            System.out.println("| Lignes retenues (majoritaires) :                 |");
            for (String line : majority) {
                System.out.println("|   " + line);
            }
        }
        System.out.println("+==================================================+");

        channel.close();
        connection.close();
    }

    /** Extrait le numéro de ligne depuis "N texte..." → N */
    private static int extractLineNumber(String line) {
        try {
            return Integer.parseInt(line.trim().split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
