package com.example.javaeeproject;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    private int availableQuantity;
    private boolean isActive = true;

    @ManyToOne
    private CompanyRepresentative companyRep;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public int getAvailableQuantity() {
        return availableQuantity;
    }
    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean active) {
        isActive = active;
    }
    public CompanyRepresentative getCompanyRep() {
        return companyRep;
    }
    public void setCompanyRep(CompanyRepresentative companyRep) {
        this.companyRep = companyRep;
    }

}