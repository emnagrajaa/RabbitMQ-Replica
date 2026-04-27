package com.tp.replication;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Processus ClientReader (questions 4 & 5).
 * Envoie une requête "READ_LAST" à tous les replicas via read_exchange (fanout)
 * et affiche la PREMIÈRE réponse reçue.
 *
 * Cela simule la disponibilité : même si un replica est en panne,
 * les autres répondent et le client obtient tout de même ses données.
 *
 * Usage : java -cp <jar> com.tp.replication.ClientReader
 */
public class ClientReader {

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQConfig.HOST);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Déclarer le exchange de lecture
        channel.exchangeDeclare(RabbitMQConfig.READ_EXCHANGE, BuiltinExchangeType.FANOUT);

        // Queue de réponse exclusive et auto-supprimée (temporaire)
        String replyQueue = channel.queueDeclare().getQueue();
        String correlationId = UUID.randomUUID().toString();

        // File bloquante pour recevoir les réponses de façon asynchrone
        BlockingQueue<String> responses = new LinkedBlockingQueue<>();

        // Consommer les réponses des replicas
        channel.basicConsume(replyQueue, true,
            (consumerTag, delivery) -> {
                if (correlationId.equals(delivery.getProperties().getCorrelationId())) {
                    String resp = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    responses.offer(resp);
                }
            },
            consumerTag -> {}
        );

        // Envoyer READ_LAST à tous les replicas
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .replyTo(replyQueue)
                .correlationId(correlationId)
                .build();

        channel.basicPublish(RabbitMQConfig.READ_EXCHANGE, "", props,
                "READ_LAST".getBytes(StandardCharsets.UTF_8));

        System.out.println("[ClientReader] >> Requete READ_LAST envoyee a tous les replicas.");
        System.out.println("[ClientReader] En attente de la premiere reponse (timeout: "
                + RabbitMQConfig.REPLY_TIMEOUT_MS + " ms)...");

        // Prendre la première réponse (premier replica disponible)
        String firstResponse = responses.poll(RabbitMQConfig.REPLY_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        System.out.println();
        if (firstResponse != null) {
            System.out.println("+----------------------------------------------+");
            System.out.println("|  RESULTAT  READ_LAST                         |");
            System.out.println("+----------------------------------------------+");
            System.out.println("|  Derniere ligne : " + firstResponse);
            System.out.println("+----------------------------------------------+");
        } else {
            System.out.println("[ClientReader] AUCUNE reponse recue (tous les replicas sont en panne ?).");
        }

        channel.close();
        connection.close();
    }
}
