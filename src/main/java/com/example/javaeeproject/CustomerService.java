package com.example.javaeeproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Stateless
public class CustomerService {
    @Inject
    private EntityManager em;

    @Inject
    private PaymentService paymentService;

    public void register(Customer customer) {
        customer.setRegistrationDate(new Date());
        em.persist(customer);
    }

    public Customer login(String email, String password) {
        return em.createQuery("SELECT c FROM Customer c WHERE c.email = :email AND c.password = :pwd", Customer.class)
                .setParameter("email", email)
                .setParameter("pwd", password)
                .getSingleResult();
    }

    public List<Order> getCurrentAndPastOrders(Long customerId) {
        return em.createQuery("SELECT o FROM Order o WHERE o.customer.id = :cid", Order.class)
                .setParameter("cid", customerId)
                .getResultList();
    }

    public Order placeOrder(Long customerId, List<OrderItem> items) {
        Customer customer = em.find(Customer.class, customerId);
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(new Date());
        order.setStatus("PENDING");
        order.setItems(new ArrayList<>());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal minimumCharge = new BigDecimal("5.00");

        for (OrderItem item : items) {
            Dish dish = em.find(Dish.class, item.getDish().getId());
            if (dish == null || dish.getAvailableQuantity() < item.getQuantity()) {
                order.setStatus("CANCELLED");
                em.persist(order);
                throw new IllegalArgumentException("Insufficient stock for dish: " + (dish != null ? dish.getName() : ""));
            }
            totalAmount = totalAmount.add(BigDecimal.valueOf(dish.getPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        if (totalAmount.compareTo(minimumCharge) < 0) {
            order.setStatus("CANCELLED");
            em.persist(order);
            throw new IllegalArgumentException("Order total must be at least $" + minimumCharge);
        }

        if (!paymentService.processPayment(totalAmount)) {
            order.setStatus("CANCELLED");
            em.persist(order);
            throw new RuntimeException("Payment failed");
        }

        for (OrderItem item : items) {
            Dish dish = em.find(Dish.class, item.getDish().getId());
            dish.setAvailableQuantity(dish.getAvailableQuantity() - item.getQuantity());
            em.merge(dish);

            item.setOrder(order);
            item.setPriceAtPurchase(dish.getPrice());
            order.getItems().add(item);
        }

        if (!items.isEmpty()) {
            Dish firstDish = items.get(0).getDish();
            order.setShippingCompanyName(firstDish.getCompanyRep().getCompanyName());
        }

        order.setStatus("CONFIRMED");
        em.persist(order);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String orderJson = mapper.writeValueAsString(order);
            try (Connection conn = RabbitMQService.getConnection();
                 Channel channel = conn.createChannel()) {

                channel.exchangeDeclare("order_exchange", "direct", true);
                channel.basicPublish("order_exchange", "OrderPlaced", null, orderJson.getBytes());

                System.out.println("Order published to RabbitMQ: " + order.getId());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return order;
    }

    public void confirmOrder(Long orderId) {
        Order order = em.find(Order.class, orderId);
        order.setStatus("CONFIRMED");
        em.merge(order);
    }
}
