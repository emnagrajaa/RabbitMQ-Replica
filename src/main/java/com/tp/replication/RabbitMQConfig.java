package com.tp.replication;

/**
 * Configuration partagée pour tous les processus RabbitMQ.
 */
public class RabbitMQConfig {

    public static final String HOST = "localhost";

    /** Exchange fanout pour diffuser les écritures à tous les replicas */
    public static final String WRITE_EXCHANGE = "write_exchange";

    /** Exchange fanout pour diffuser les requêtes de lecture à tous les replicas */
    public static final String READ_EXCHANGE = "read_exchange";

    /** Préfixe du nom de la queue de chaque replica */
    public static final String QUEUE_PREFIX = "replica_queue_";

    /** Nombre total de replicas */
    public static final int REPLICA_COUNT = 3;

    /** Timeout d'attente de réponse en millisecondes */
    public static final int REPLY_TIMEOUT_MS = 4000;
}
