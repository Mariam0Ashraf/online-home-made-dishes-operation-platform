package com.example.javaeeproject;

import com.rabbitmq.client.*;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@Startup
@Path("/logs")
public class AdminNotificationService {

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

    public AdminNotificationService() {
        try {
            start();
        } catch (Exception e) {
            logs.add("ERROR: Failed to start AdminNotificationService - " + e.getMessage());
        }
    }

    public void start() throws Exception {
        Connection conn = RabbitMQService.getConnection();
        Channel channel = conn.createChannel();
        channel.exchangeDeclare("payment_exchange", "direct", true);
        String queue1 = channel.queueDeclare().getQueue();
        channel.queueBind(queue1, "payment_exchange", "PaymentFailed");

        channel.exchangeDeclare("log_exchange", "topic", true);
        String queue2 = channel.queueDeclare().getQueue();
        channel.queueBind(queue2, "log_exchange", "*.Error");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logs.add(msg);
            System.out.println("Admin log: " + msg);
        };

        channel.basicConsume(queue1, true, callback, consumerTag -> {});
        channel.basicConsume(queue2, true, callback, consumerTag -> {});
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getLogs() {
        return logs == null ? new ArrayList<>() : logs;
    }
}
