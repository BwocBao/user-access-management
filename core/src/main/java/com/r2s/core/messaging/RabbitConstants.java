package com.r2s.core.messaging;

public class RabbitConstants {

//    public static final String USER_CREATED = "user.created";
//    public static final String USER_DELETED = "user.deleted";
//
//    // ===== MAIN =====
//    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
//
//    // ===== DLQ =====
//    public static final String PRODUCER_USER_REGISTERED_DLQ = "producer.user.registered.dlq";
//    public static final String PRODUCER_USER_REGISTERED_DLX = "producer.user.registered.dlx";
//    public static final String PRODUCER_USER_REGISTERED_DL_ROUTING = "producer.user.registered.dl.routing";
//    public static final String CONSUMER_USER_REGISTERED_DLQ = "comsumer.user.registered.dlq";
//    public static final String CONSUMER_USER_REGISTERED_DLX = "consumer.user.registered.dlx";
//    public static final String CONSUMER_USER_REGISTERED_DL_ROUTING = "consumer.user.registered.dl.routing";
//
//    // ===== RETRY =====
//    public static final String PRODUCER_USER_REGISTERED_RETRY_EXCHANGE = "producer.user.registered.retry.exchange";
//    public static final String PRODUCER_USER_REGISTERED_RETRY_ROUTING = "producer.user.registered.retry";
//    public static final String PRODUCER_USER_REGISTERED_RETRY_QUEUE = "producer.user.registered.retry.queue";
//    public static final String CONSUMER_USER_REGISTERED_RETRY_EXCHANGE = "consumer.user.registered.retry.exchange";
//    public static final String CONSUMER_USER_REGISTERED_RETRY_ROUTING = "consumer.user.registered.retry.routing";
//    public static final String CONSUMER_USER_REGISTERED_RETRY_QUEUE = "consumer.user.registered.retry.queue";
//
//    // ===== CORE EXCHANGE =====
//    public static final String USER_EXCHANGE = "user.exchange";
//    public static final String USER_REGISTERED_ROUTING = "user.registered";
//
//    // ===== CONFIG =====
//    public static final int RETRY_TTL = 5000;
//    public static final String HEADER_RETRY_COUNT = "x-retry-count";
//    public static final String HEADER_USER_REGISTERED_PUBLISH_RETRY = "x-publish-retry";
//    public static final int MAX_RETRY = 3;

    // ===== CORE =====
    // ===== DOMAIN =====
    public static final String USER = "user";

    // ===== ACTION =====
    public static final String REGISTERED = "registered";
    public static final String DELETED= "deleted";

    // ===== EVENT =====
    public static final String USER_REGISTERED = USER + "." + REGISTERED;
    public static final String USER_DELETED = USER + "." + DELETED;

    // ===== SUFFIX =====
    public static final String EXCHANGE_SUFFIX = ".exchange";
    public static final String EXCHANGE_SUFFIX_DLQ = ".dlq.exchange";
    public static final String EXCHANGE_SUFFIX_RETRY = ".retry.exchange";

    public static final String ROUTING_SUFFIX = ".routing";
    public static final String ROUTING_SUFFIX_DLQ = ".dlq.routing";
    public static final String ROUTING_SUFFIX_RETRY = ".retry.routing";

    public static final String QUEUE_SUFFIX = ".queue";
    public static final String RETRY_QUEUE_SUFFIX = ".retry.queue";
    public static final String DLQ_QUEUE_SUFFIX = ".dlq.queue";
    // ===== EXCHANGE =====
    public static final String USER_EXCHANGE = USER + EXCHANGE_SUFFIX;
    public static final String USER_RETRY_EXCHANGE = USER + EXCHANGE_SUFFIX_RETRY;
    public static final String USER_DLQ_EXCHANGE = USER + EXCHANGE_SUFFIX_DLQ;

    // ===== ROUTING =====
    public static final String USER_REGISTERED_ROUTING =
            USER_REGISTERED + ROUTING_SUFFIX;

    public static final String USER_REGISTERED_RETRY_ROUTING =
            USER_REGISTERED + ROUTING_SUFFIX_RETRY;

    public static final String USER_REGISTERED_DLQ_ROUTING =
            USER_REGISTERED + ROUTING_SUFFIX_DLQ;

    public static final String USER_DELETED_ROUTING = USER_DELETED + ROUTING_SUFFIX ;

    public static final String USER_DELETED_RETRY_ROUTING = USER_DELETED + ROUTING_SUFFIX_RETRY;

    public static final String USER_DELETED_DLQ_ROUTING = USER_DELETED + ROUTING_SUFFIX_DLQ;
    // ===== QUEUE =====
    public static final String USER_REGISTERED_QUEUE =
            USER_REGISTERED + QUEUE_SUFFIX;

    public static final String USER_REGISTERED_RETRY_QUEUE =
            USER_REGISTERED + RETRY_QUEUE_SUFFIX;

    public static final String USER_REGISTERED_DLQ_QUEUE =
            USER_REGISTERED + DLQ_QUEUE_SUFFIX;

    public static final String USER_DELETED_QUEUE = USER_DELETED + QUEUE_SUFFIX;

    public static final String USER_DELETED_RETRY_QUEUE = USER_DELETED + RETRY_QUEUE_SUFFIX;

    public static final String USER_DELETED_DLQ_QUEUE = USER_DELETED + DLQ_QUEUE_SUFFIX;

    // ===== HEADER =====
    public static final String HEADER_CONSUME_RETRY_COUNT = "x-retry-count";
    public static final String HEADER_PUBLISH_RETRY_COUNT = "x-publish-retry";

    // ===== CONFIG =====
    public static final int RETRY_TTL_MS = 5000;
    public static final int MAX_RETRY = 3;
}