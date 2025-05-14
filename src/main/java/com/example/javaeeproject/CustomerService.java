package com.example.javaeeproject;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Stateless
public class CustomerService {
    @Inject
    private EntityManager em;

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
        Order order = new Order();
        order.setCustomer(em.find(Customer.class, customerId));
        order.setOrderDate(new Date());
        order.setStatus("PENDING");
        order.setItems(new ArrayList<>());

        for (OrderItem item : items) {
            item.setOrder(order);
            item.setPriceAtPurchase(item.getDish().getPrice());
            order.getItems().add(item);
        }

        em.persist(order);
        return order;
    }

    public void confirmOrder(Long orderId) {
        Order order = em.find(Order.class, orderId);
        order.setStatus("CONFIRMED");
        em.merge(order);
    }
}