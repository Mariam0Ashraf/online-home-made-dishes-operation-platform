package com.example.javaeeproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
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

    private static final String ORDER_QUEUE = "order_queue";
    private static final String INVENTORY_EXCHANGE = "inventory_exchange";
    private static final String LOG_EXCHANGE = "log_exchange";

    @PostConstruct
    public void init() {
        new Thread(() -> {
            try {
                start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public void start() throws Exception {
        Connection connection = RabbitMQService.getConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(ORDER_QUEUE, false, false, false, null);

        System.out.println(" [*] InventoryService waiting for orders...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received Order: " + messageBody);

            try {
                ObjectMapper mapper = new ObjectMapper();
                Order order = mapper.readValue(messageBody, Order.class);
                boolean stockAvailable = checkAndReserveStock(order.getItems());

                if (stockAvailable) {
                    channel.basicPublish(INVENTORY_EXCHANGE, "CheckStock", null, messageBody.getBytes());
                    log(channel, "Inventory_Info", "Stock confirmed for Order #" + order.getId());
                } else {
                    log(channel, "Inventory_Error", "Stock insufficient for Order #" + order.getId());
                }

            } catch (Exception e) {
                e.printStackTrace();
                log(channel, "Inventory_Error", "Exception while checking stock.");
            }
        };
        channel.basicConsume(ORDER_QUEUE, true, deliverCallback, consumerTag -> {});
    }
    @Transactional
    public boolean checkAndReserveStock(List<OrderItem> items) {
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
    private void log(Channel channel, String severity, String message) throws IOException {
        channel.exchangeDeclare(LOG_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        channel.basicPublish(LOG_EXCHANGE, severity, null, message.getBytes(StandardCharsets.UTF_8));
    }
}
