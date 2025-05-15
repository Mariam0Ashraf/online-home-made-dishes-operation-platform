package com.example.javaeeproject;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
public class AdminService {
    @PersistenceContext
    private EntityManager em;

    public List<CompanyRepresentative> createCompanyRepresentatives(List<String> companyNames) {
        List<CompanyRepresentative> reps = new ArrayList<>(); // Fixed: Collect created reps
        for (String name : companyNames) {
            CompanyRepresentative rep = new CompanyRepresentative();
            rep.setCompanyName(name);
            String password = UUID.randomUUID().toString();
            rep.setPassword(password);
            em.persist(rep);
            reps.add(rep); // Add to list
            System.out.println("Sent password " + password + " to " + name);
        }
        return reps; // Return actual list instead of null
    }

    public List<Customer> listAllCustomers() {
        return em.createQuery("SELECT c FROM Customer c", Customer.class).getResultList();
    }

    public List<CompanyRepresentative> listAllCompanyRepresentatives() {
        return em.createQuery("SELECT cr FROM CompanyRepresentative cr", CompanyRepresentative.class).getResultList();
    }
}