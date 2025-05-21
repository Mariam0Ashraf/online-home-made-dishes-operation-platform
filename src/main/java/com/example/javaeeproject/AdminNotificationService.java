package com.example.javaeeproject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.nio.charset.StandardCharsets;

@Singleton
@Startup
public class AdminNotificationService {

    public void start() throws Exception {
        Connection conn = RabbitMQService.getConnection();
        Channel channel = conn.createChannel();

        channel.exchangeDeclare("payment_exchange", "direct", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, "payment_exchange", "PaymentFailed");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Admin alert: Payment failed for Order: " + msg);
        };

        channel.basicConsume(queueName, true, callback, consumerTag -> {});
    }
}

