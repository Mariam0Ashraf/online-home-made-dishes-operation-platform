package com.example.javaeeproject;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
@Stateless
public class CompanyRepresentativeService {
    @PersistenceContext
    private EntityManager em;

    private static CompanyRepresentative loggedInRep = null;

    public CompanyRepresentative login(String companyName, String password) {
        CompanyRepresentative rep = em.createQuery(
                        "SELECT cr FROM CompanyRepresentative cr WHERE cr.companyName = :name AND cr.password = :pwd", CompanyRepresentative.class)
                .setParameter("name", companyName)
                .setParameter("pwd", password)
                .getSingleResult();
        loggedInRep = rep;
        return rep;
    }

    public boolean isLoggedIn() {
        return loggedInRep != null;
    }

    public Long getLoggedInRepId() {
        return loggedInRep != null ? loggedInRep.getId() : null;
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
        Long repId = getLoggedInRepId();
        if (repId == null) throw new IllegalStateException("Not logged in");

        CompanyRepresentative rep = em.find(CompanyRepresentative.class, repId);
        dish.setCompanyRep(rep);
        em.persist(dish);
    }


    public void updateDish(Dish dish) {
        Long repId = getLoggedInRepId();
        if (repId == null) throw new IllegalStateException("Not logged in");

        CompanyRepresentative rep = em.find(CompanyRepresentative.class, repId);
        dish.setCompanyRep(rep);
        em.merge(dish);
    }

}
