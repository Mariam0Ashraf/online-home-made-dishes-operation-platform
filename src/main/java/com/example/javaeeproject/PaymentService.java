package com.example.javaeeproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Singleton
@Startup
public class PaymentService {
    private static final String INVENTORY_QUEUE = "inventory_queue";
    private static final String LOG_EXCHANGE = "log_exchange";
    private static final String NOTIFICATIONS_EXCHANGE = "notifications_exchange";

    @PersistenceContext
    private EntityManager em;

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
        channel.queueDeclare(INVENTORY_QUEUE, false, false, false, null);

        System.out.println(" [*] PaymentService waiting for inventory confirmation...");
        DeliverCallback callback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received stock-confirmed order: " + message);

            try {
                ObjectMapper mapper = new ObjectMapper();
                Order order = mapper.readValue(message, Order.class);

                boolean paymentSuccess = processPayment(order);

                if (paymentSuccess) {
                    log(channel, "Payments_Info", "Payment processed for Order #" + order.getId());
                } else {
                    log(channel, "Payments_Error", "Payment failed for Order #" + order.getId());
                    channel.basicPublish(NOTIFICATIONS_EXCHANGE, "PaymentFailed", null, message.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                log(channel, "Payments_Error", "Exception while processing payment.");
            }
        };
        channel.basicConsume(INVENTORY_QUEUE, true, callback, tag -> {});
    }

    public boolean processPayment(Order order) {
        Customer customer = em.find(Customer.class, order.getCustomer().getId());
        double total = order.getItems().stream()
                .mapToDouble(i -> i.getQuantity() * i.getPriceAtPurchase())
                .sum();

        if (customer.getBalance() >= total) {
            customer.setBalance(customer.getBalance() - total);
            em.merge(customer);
            return true;
        } else {
            return false;
        }
    }

    private void log(Channel channel, String severity, String message) throws IOException {
        channel.exchangeDeclare(LOG_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        channel.basicPublish(LOG_EXCHANGE, severity, null, message.getBytes(StandardCharsets.UTF_8));
    }
}