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

@Singleton
@Startup
public class PaymentService {

    @PersistenceContext
    private EntityManager em;

    private static final double MINIMUM_CHARGE = 5.0;

    public void start() throws Exception {
        Connection conn = RabbitMQService.getConnection();
        Channel channel = conn.createChannel();

        channel.exchangeDeclare("payment_exchange", "direct", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, "payment_exchange", "ProcessPayment");

        DeliverCallback callback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Order order = new ObjectMapper().readValue(msg, Order.class);

            double total = order.getItems().stream()
                    .mapToDouble(i -> i.getQuantity() * i.getPriceAtPurchase())
                    .sum();

            if (total < MINIMUM_CHARGE) {
                cancelOrderAndRollbackStock(order, channel);
                log(channel, "Payments_Error", "Order " + order.getId() + " failed: below minimum charge");
                return;
            }

            boolean paymentSuccess = processPayment(order);
            if (paymentSuccess) {
                updateOrderStatus(order.getId(), "CONFIRMED");
                publishPaymentEvent(order, "OrderConfirmed");
                log(channel, "Payments_Info", "Payment successful for Order " + order.getId());
            } else {
                cancelOrderAndRollbackStock(order, channel);
                publishPaymentEvent(order, "PaymentFailed");
                log(channel, "Payments_Error", "Payment failed for Order " + order.getId());
            }
        };
        channel.basicConsume(queueName, true, callback, consumerTag -> {});
    }

    @Transactional
    private boolean processPayment(Order order) {
        Customer customer = em.find(Customer.class, order.getCustomer().getId());
        double total = order.getItems().stream()
                .mapToDouble(i -> i.getQuantity() * i.getPriceAtPurchase())
                .sum();

        if (customer.getBalance() >= total) {
            customer.setBalance(customer.getBalance() - total);
            em.merge(customer);
            return true;
        }
        return false;
    }

    @Transactional
    private void cancelOrderAndRollbackStock(Order order, Channel channel) throws IOException {
        updateOrderStatus(order.getId(), "CANCELLED");

        for (OrderItem item : order.getItems()) {
            Dish dish = em.find(Dish.class, item.getDish().getId());
            dish.setAvailableQuantity(dish.getAvailableQuantity() + item.getQuantity());
            em.merge(dish);
        }
        log(channel, "Payments_Error", "Stock rolled back for Order " + order.getId());
    }

    private void updateOrderStatus(Long orderId, String status) {
        Order order = em.find(Order.class, orderId);
        order.setStatus(status);
        em.merge(order);
    }

    private void publishPaymentEvent(Order order, String routingKey) {
        try (Connection conn = RabbitMQService.getConnection();
             Channel channel = conn.createChannel()) {
            channel.exchangeDeclare("payment_exchange", "direct", true);
            String json = new ObjectMapper().writeValueAsString(order);
            channel.basicPublish("payment_exchange", routingKey, null, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(Channel channel, String severity, String message) throws IOException {
        channel.exchangeDeclare("log_exchange", BuiltinExchangeType.TOPIC, true);
        channel.basicPublish("log_exchange", severity, null, message.getBytes(StandardCharsets.UTF_8));
    }
}
