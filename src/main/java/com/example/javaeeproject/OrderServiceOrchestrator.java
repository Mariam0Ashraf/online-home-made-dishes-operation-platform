package com.example.javaeeproject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.nio.charset.StandardCharsets;
@Singleton
@Startup
public class OrderServiceOrchestrator {

    @PersistenceContext
    private EntityManager em;

    public void start() throws Exception {
        Connection conn = RabbitMQService.getConnection();
        Channel channel = conn.createChannel();

        channel.exchangeDeclare("order_exchange", "direct", true);
        String queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, "order_exchange", "StockConfirmed");
        channel.queueBind(queueName, "order_exchange", "StockRejected");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Order order = new ObjectMapper().readValue(msg, Order.class);

            if ("StockConfirmed".equals(delivery.getEnvelope().getRoutingKey())) {
                channel.basicPublish("payment_exchange", "ProcessPayment", null, msg.getBytes(StandardCharsets.UTF_8));
                updateOrderStatus(order.getId(), "PENDING_PAYMENT");
            } else {
                updateOrderStatus(order.getId(), "CANCELLED");
            }
        };

        channel.basicConsume(queueName, true, callback, consumerTag -> {});
    }

    private void updateOrderStatus(Long orderId, String status) {
        Order order = em.find(Order.class, orderId);
        order.setStatus(status);
        em.merge(order);
    }
}
