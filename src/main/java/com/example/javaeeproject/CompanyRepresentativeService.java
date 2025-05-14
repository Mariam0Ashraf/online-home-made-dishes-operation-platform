package com.example.javaeeproject;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;

@Stateless
public class CompanyRepresentativeService {
    @Inject
    private EntityManager em;

    public CompanyRepresentative login(String companyName, String password) {
        return em.createQuery("SELECT cr FROM CompanyRepresentative cr WHERE cr.companyName = :name AND cr.password = :pwd", CompanyRepresentative.class)
                .setParameter("name", companyName)
                .setParameter("pwd", password)
                .getSingleResult();
    }

    public List<Dish> getCurrentDishes(Long repId) {
        return em.createQuery("SELECT d FROM Dish d WHERE d.companyRep.id = :repId AND d.isActive = true", Dish.class)
                .setParameter("repId", repId)
                .getResultList();
    }

    public List<Order> getSoldDishesWithCustomerAndShipping(Long repId) {
        return em.createQuery("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.dish.companyRep.id = :repId", Order.class)
                .setParameter("repId", repId)
                .getResultList();
    }

    public void addDish(Dish dish) {
        em.persist(dish);
    }

    public void updateDish(Dish dish) {
        em.merge(dish);
    }
}
