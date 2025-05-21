package com.example.javaeeproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class CustomerService {
    @PersistenceContext
    private EntityManager em;
    private static Customer loggedInCustomer = null;

    @Inject
    private PaymentService paymentService;

    public void register(Customer customer) {
        if (customer.getBalance() < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        customer.setRegistrationDate(new Date());
        em.persist(customer);
        customer.setRegistrationDate(new Date());
        em.persist(customer);
    }

    public Customer login(String email, String password) {
        Customer customer = em.createQuery(
                        "SELECT c FROM Customer c WHERE c.email = :email AND c.password = :pwd", Customer.class)
                .setParameter("email", email)
                .setParameter("pwd", password)
                .getSingleResult();
        loggedInCustomer = customer;
        return customer;
    }

    public boolean isLoggedIn() {
        return loggedInCustomer != null;
    }

    public Long getLoggedInCustomerId() {
        return loggedInCustomer != null ? loggedInCustomer.getId() : null;
    }

    public List<OrderDTO> getCurrentAndPastOrders(Long customerId) {
        List<Order> orders = em.createQuery("SELECT o FROM Order o WHERE o.customer.id = :cid", Order.class)
                .setParameter("cid", customerId)
                .getResultList();

        return orders.stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
    }

    public OrderDTO placeOrder(Long customerId, List<OrderItemDTO> itemsDto) {
        if (!isLoggedIn() || !customerId.equals(getLoggedInCustomerId())) {
            throw new IllegalArgumentException("You must be logged in as this customer");
        }

        Order order = new Order();
        Customer customer = em.find(Customer.class, customerId);
        order.setCustomer(customer);
        order.setOrderDate(new Date());
        order.setStatus("PLACED");
        order.setItems(new ArrayList<>());
        em.persist(order);

        for (OrderItemDTO dto : itemsDto) {
            Dish dish = em.find(Dish.class, dto.getDishId());
            if (dish == null || !dish.isActive()) {
                throw new IllegalArgumentException("Dish with id " + dto.getDishId() + " not found");
            }
            if (dish.getAvailableQuantity() < dto.getQuantity()) {
                throw new IllegalArgumentException("Not enough quantity for dish " + dish.getName());
            }

            dish.setAvailableQuantity(dish.getAvailableQuantity() - dto.getQuantity());
            em.merge(dish);

            OrderItem item = new OrderItem();
            item.setDish(dish);
            item.setQuantity(dto.getQuantity());
            item.setPriceAtPurchase(dish.getPrice());
            item.setOrder(order);
            em.persist(item);

            order.getItems().add(item);
        }

        em.merge(order);
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
        return new OrderDTO(order);
    }

    public void confirmOrder(Long orderId) {
        Order order = em.find(Order.class, orderId);
        order.setStatus("CONFIRMED");
        em.merge(order);
    }

    public List<Dish> getAllActiveDishes() {
        return em.createQuery("SELECT d FROM Dish d WHERE d.isActive = true", Dish.class)
                .getResultList();
    }
}