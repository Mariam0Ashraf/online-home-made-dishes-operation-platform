package com.example.javaeeproject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class AdminNotificationConsumer {
    public static void main(String[] args) throws Exception {
        Connection conn = RabbitMQService.getConnection();
        Channel channel = conn.createChannel();

        channel.basicConsume("payment_failed_queue", true, (tag, msg) -> {
            System.out.println("Admin ALERT:"+new String(msg.getBody()));
        }, tag -> {});

        channel.basicConsume("admin_log_queue", true, (tag, msg) -> {
            System.out.println("Admin LOG: " + new String(msg.getBody()));
        }, tag -> {});
    }
}
