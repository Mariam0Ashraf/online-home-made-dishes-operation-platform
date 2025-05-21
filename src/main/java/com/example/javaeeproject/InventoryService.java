package com.example.javaeeproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Singleton
@Startup
public class InventoryService {
    @PersistenceContext
    private EntityManager em;

    public void start() throws Exception {
        Connection conn = RabbitMQService.getConnection();
        Channel channel = conn.createChannel();

        channel.exchangeDeclare("order_exchange", "direct", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, "order_exchange", "OrderCreated");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Order order = new ObjectMapper().readValue(msg, Order.class);

            if (checkAndReserveStock(order.getItems())) {
                updateOrderStatus(order.getId(), "STOCK_CONFIRMED");
                publishInventoryEvent(order, "StockConfirmed");
                log(channel, "Inventory_Info", "Stock confirmed for Order " + order.getId());
            } else {
                updateOrderStatus(order.getId(), "CANCELLED");
                publishInventoryEvent(order, "StockRejected");
                log(channel, "Inventory_Error", "Stock rejected for Order " + order.getId());
            }
        };
        channel.basicConsume(queueName, true, callback, consumerTag -> {});
    }

    @Transactional
    private boolean checkAndReserveStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Dish dish = em.find(Dish.class, item.getDish().getId());
            if (dish == null || dish.getAvailableQuantity() < item.getQuantity()) {
                return false;
            }
        }
        for (OrderItem item : items) {
            Dish dish = em.find(Dish.class, item.getDish().getId());
            dish.setAvailableQuantity(dish.getAvailableQuantity() - item.getQuantity());
            em.merge(dish);
        }
        return true;
    }

    private void updateOrderStatus(Long orderId, String status) {
        Order order = em.find(Order.class, orderId);
        order.setStatus(status);
        em.merge(order);
    }

    private void publishInventoryEvent(Order order, String routingKey) {
        try (Connection conn = RabbitMQService.getConnection();
             Channel channel = conn.createChannel()) {
            channel.exchangeDeclare("order_exchange", "direct", true);

            String json = new ObjectMapper().writeValueAsString(order);
            channel.basicPublish("order_exchange", routingKey, null, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(Channel channel, String severity, String message) throws IOException {
        channel.exchangeDeclare("log_exchange", BuiltinExchangeType.TOPIC, true);
        channel.basicPublish("log_exchange", severity, null, message.getBytes(StandardCharsets.UTF_8));
    }
}
