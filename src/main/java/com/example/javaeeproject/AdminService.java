package com.example.javaeeproject;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;
@Stateless
public class AdminService {
    @Inject
    private EntityManager em;

    public void createCompanyRepresentatives(List<String> companyNames) {
        for (String name : companyNames) {
            CompanyRepresentative rep = new CompanyRepresentative();
            rep.setCompanyName(name);
            String password = UUID.randomUUID().toString();
            rep.setPassword(password);
            em.persist(rep);

            System.out.println("Sent password " + password + " to " + name);
        }
    }

    public List<Customer> listAllCustomers() {
        return em.createQuery("SELECT c FROM Customer c", Customer.class).getResultList();
    }

    public List<CompanyRepresentative> listAllCompanyRepresentatives() {
        return em.createQuery("SELECT cr FROM CompanyRepresentative cr", CompanyRepresentative.class).getResultList();
    }
}