package com.example.javaeeproject;

public class OrderItemDTO {
    private Long dishId;
    private int quantity;

    public Long getDishId() { return dishId; }
    public void setDishId(Long dishId) { this.dishId = dishId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}