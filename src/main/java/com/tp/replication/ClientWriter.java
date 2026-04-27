package com.tp.replication;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

/**
 * Processus ClientWriter (question 1).
 * Émet un message d'ajout de ligne vers write_exchange (fanout).
 *
 * Usage : java -cp <jar> com.tp.replication.ClientWriter <numero_ligne> <texte...>
 * Exemple: java ClientWriter 1 Texte message1
 *          java ClientWriter 2 Texte message2
 */
public class ClientWriter {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Usage: java ClientWriter <numero_ligne> <texte...>");
            System.err.println("Exemple: java ClientWriter 1 Texte message1");
            System.exit(1);
        }

        // Construire la ligne : "numero texte..."
        String lineNumber = args[0];
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(args[i]);
        }
        String fullLine = lineNumber + " " + sb;

        // Connexion et publication
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQConfig.HOST);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Déclarer le exchange (idempotent)
            channel.exchangeDeclare(RabbitMQConfig.WRITE_EXCHANGE, BuiltinExchangeType.FANOUT);

            String message = "WRITE:" + fullLine;
            channel.basicPublish(RabbitMQConfig.WRITE_EXCHANGE, "", null,
                    message.getBytes(StandardCharsets.UTF_8));

            System.out.println("[ClientWriter] >> Message envoye : " + message);
            System.out.println("[ClientWriter] Diffuse vers " + RabbitMQConfig.REPLICA_COUNT + " replicas.");
        }
    }
}
