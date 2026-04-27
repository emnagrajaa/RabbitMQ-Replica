package com.tp.replication;

import com.rabbitmq.client.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Processus Replica (questions 2, 3, 6).
 * Lancement : java -cp <jar> com.tp.replication.Replica <id>
 * Exemples   : java Replica 1 | java Replica 2 | java Replica 3
 *
 * Chaque replica :
 *  - Écoute sa propre queue replica_queue_{id}
 *  - Cette queue est liée à write_exchange ET read_exchange (fanout)
 *  - Sur WRITE   : ajoute la ligne dans son fichier local
 *  - Sur READ_LAST : renvoie la dernière ligne au client
 *  - Sur READ_ALL  : renvoie toutes les lignes puis un message "END"
 *
 * NOTE : la queue est auto-delete (disparaît quand le replica s'arrête),
 * ce qui simule la perte de messages lors d'une panne (question 6).
 */
public class Replica {

    private static int replicaId;
    private static String filePath;

    // Canal dédié à la publication des réponses (thread-safe via synchronized)
    private static Channel publishChannel;

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.err.println("Usage: java Replica <id>");
            System.exit(1);
        }

        replicaId = Integer.parseInt(args[0]);
        String dirPath = "replica_" + replicaId;
        filePath = dirPath + File.separator + "data.txt";

        // Créer le répertoire local si inexistant
        Files.createDirectories(Paths.get(dirPath));

        // Connexion RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQConfig.HOST);
        Connection connection = factory.newConnection();

        // Deux canaux : un pour consommer, un pour publier les réponses
        Channel consumeChannel = connection.createChannel();
        publishChannel = connection.createChannel();

        // Déclarer les exchanges
        consumeChannel.exchangeDeclare(RabbitMQConfig.WRITE_EXCHANGE, BuiltinExchangeType.FANOUT);
        consumeChannel.exchangeDeclare(RabbitMQConfig.READ_EXCHANGE,  BuiltinExchangeType.FANOUT);

        // Queue propre au replica : non-durable, auto-delete → simuler pannes
        String queueName = RabbitMQConfig.QUEUE_PREFIX + replicaId;
        consumeChannel.queueDeclare(queueName, false, false, true, null);

        // Lier à write_exchange ET read_exchange
        consumeChannel.queueBind(queueName, RabbitMQConfig.WRITE_EXCHANGE, "");
        consumeChannel.queueBind(queueName, RabbitMQConfig.READ_EXCHANGE,  "");

        printBanner();

        // Consommation des messages
        consumeChannel.basicConsume(queueName, true,
            (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String replyTo = delivery.getProperties().getReplyTo();
                String correlationId = delivery.getProperties().getCorrelationId();

                System.out.println("[Replica " + replicaId + "] << Recu: " + message);

                try {
                    if (message.startsWith("WRITE:")) {
                        // ---- ECRITURE ----
                        String line = message.substring(6);
                        appendLine(line);
                        System.out.println("[Replica " + replicaId + "] OK ecrit: " + line);

                    } else if (message.equals("READ_LAST")) {
                        // ---- LECTURE DERNIERE LIGNE ----
                        String lastLine = readLastLine();
                        String response = (lastLine != null) ? lastLine : "EMPTY";
                        System.out.println("[Replica " + replicaId + "] >> READ_LAST => " + response);
                        if (replyTo != null) {
                            sendReply(replyTo, correlationId, response);
                        }

                    } else if (message.equals("READ_ALL")) {
                        // ---- LECTURE TOTALE (pour ClientReaderV2) ----
                        List<String> lines = readAllLines();
                        System.out.println("[Replica " + replicaId + "] >> READ_ALL => " + lines.size() + " ligne(s)");
                        if (replyTo != null) {
                            for (String line : lines) {
                                sendReply(replyTo, correlationId, "LINE:" + line);
                            }
                            sendReply(replyTo, correlationId, "END");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[Replica " + replicaId + "] ERREUR: " + e.getMessage());
                }
            },
            consumerTag -> System.out.println("[Replica " + replicaId + "] Consumer annule.")
        );

        // Maintenir le processus actif
        Thread.currentThread().join();
    }

    // ------------------------------------------------------------------ //
    //  Opérations fichier (synchronized pour la sécurité des threads)     //
    // ------------------------------------------------------------------ //

    private static synchronized void appendLine(String line) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath, true))) {
            w.write(line);
            w.newLine();
        }
    }

    private static synchronized String readLastLine() throws IOException {
        File file = new File(filePath);
        if (!file.exists()) return null;
        String last = null;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) last = line;
            }
        }
        return last;
    }

    private static synchronized List<String> readAllLines() throws IOException {
        File file = new File(filePath);
        if (!file.exists()) return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line);
            }
        }
        return lines;
    }

    private static synchronized void sendReply(String replyTo, String correlationId,
                                               String message) throws IOException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .build();
        publishChannel.basicPublish("", replyTo, props,
                message.getBytes(StandardCharsets.UTF_8));
    }

    private static void printBanner() {
        System.out.println("+------------------------------------------+");
        System.out.println("|  Replica " + replicaId + " demarre                       |");
        System.out.println("|  Queue : " + RabbitMQConfig.QUEUE_PREFIX + replicaId + "         |");
        System.out.println("|  Fichier: " + filePath + "              |");
        System.out.println("+------------------------------------------+");
    }
}
