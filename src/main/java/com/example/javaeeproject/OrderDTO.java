package com.example.javaeeproject;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class OrderDTO {
    private Long id;
    private Long customerId;
    private Date orderDate;
    private String status;
    private String shippingCompanyName;
    private List<OrderItemDTO> items;

    public OrderDTO() {}

    public OrderDTO(Order order) {
        this.id = order.getId();
        this.customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
        this.orderDate = order.getOrderDate();
        this.status = order.getStatus();
        this.shippingCompanyName = order.getShippingCompanyName();
        this.items = order.getItems().stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
    }

    private OrderItemDTO convertToOrderItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setDishId(item.getDish() != null ? item.getDish().getId() : null);
        dto.setQuantity(item.getQuantity());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShippingCompanyName() {
        return shippingCompanyName;
    }

    public void setShippingCompanyName(String shippingCompanyName) {
        this.shippingCompanyName = shippingCompanyName;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
}