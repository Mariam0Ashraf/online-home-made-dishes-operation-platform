package com.example.javaeeproject;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Stateless
public class AdminService {
    @Inject
    private EntityManager em;

    public List<CompanyRepresentative> createCompanyRepresentatives(List<String> companyNames) {
        List<CompanyRepresentative> createdReps = new ArrayList<>();

        for (String name : companyNames) {
            CompanyRepresentative rep = new CompanyRepresentative();
            rep.setCompanyName(name);
            String generatedPassword = UUID.randomUUID().toString().substring(0, 8);
            rep.setPassword(generatedPassword);

            em.persist(rep);
            createdReps.add(rep);
        }
        return createdReps;
    }

    public List<Customer> listAllCustomers() {
        return em.createQuery("SELECT c FROM Customer c", Customer.class).getResultList();
    }

    public List<CompanyRepresentative> listAllCompanyRepresentatives() {
        return em.createQuery("SELECT cr FROM CompanyRepresentative cr", CompanyRepresentative.class).getResultList();
    }
}
