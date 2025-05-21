package com.example.javaeeproject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQService {

    static Connection getConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory.newConnection();
    }

    public static void main(String[] args) throws Exception {
        try (Connection conn = getConnection();
             Channel channel = conn.createChannel()) {

            channel.exchangeDeclare("order_exchange", "direct", true);
            channel.exchangeDeclare("inventory_exchange", "direct", true);
            channel.exchangeDeclare("payments_exchange", "direct", true);
            channel.exchangeDeclare("log_exchange", "topic", true);
            channel.exchangeDeclare("notifications_exchange", "direct", true);

            channel.queueDeclare("order_queue", false, false, false, null);
            channel.queueDeclare("inventory_queue", false, false, false, null);
            channel.queueDeclare("payment_queue", false, false, false, null);
            channel.queueDeclare("payment_failed_queue", false, false, false, null);
            channel.queueDeclare("admin_log_queue", false, false, false, null);
            channel.queueDeclare("customer_notification_queue", false, false, false, null);

            channel.queueBind("order_queue", "order_exchange", "OrderPlaced");
            channel.queueBind("inventory_queue", "inventory_exchange", "CheckStock");
            channel.queueBind("payment_queue", "payments_exchange", "ProcessPayment");
            channel.queueBind("payment_failed_queue", "notifications_exchange", "PaymentFailed");
            channel.queueBind("admin_log_queue", "log_exchange", "*.Error");

            System.out.println("RabbitMQ setup complete.");
        }
    }
}
