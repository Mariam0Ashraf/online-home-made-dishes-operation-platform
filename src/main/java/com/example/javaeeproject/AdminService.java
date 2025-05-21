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

    private static boolean isAdminLoggedIn = false;

    public List<CompanyRepresentative> createCompanyRepresentatives(List<String> companyNames) {
        List<CompanyRepresentative> reps = new ArrayList<>();
        for (String name : companyNames) {
            CompanyRepresentative rep = new CompanyRepresentative();
            rep.setCompanyName(name);
            String password = UUID.randomUUID().toString();
            rep.setPassword(password);
            em.persist(rep);
            reps.add(rep);
            System.out.println("Sent password " + password + " to " + name);
        }
        return reps;
    }

    public List<Customer> listAllCustomers() {
        return em.createQuery("SELECT c FROM Customer c", Customer.class).getResultList();
    }

    public List<CompanyRepresentative> listAllCompanyRepresentatives() {
        return em.createQuery("SELECT cr FROM CompanyRepresentative cr", CompanyRepresentative.class).getResultList();
    }

    public boolean login(String username, String password) {
        List<Admin> admins = em.createQuery(
                        "SELECT a FROM Admin a WHERE a.username = :username AND a.password = :password", Admin.class)
                .setParameter("username", username)
                .setParameter("password", password)
                .getResultList();
        if (!admins.isEmpty()) {
            isAdminLoggedIn = true;
            return true;
        }
        return false;
    }

    public boolean isLoggedIn() {
        return isAdminLoggedIn;
    }

    public void initializeAdmins() {
        String[] usernames = {"admin1", "admin2", "admin3"};
        for (String username : usernames) {
            long count = em.createQuery(
                            "SELECT COUNT(a) FROM Admin a WHERE a.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult();
            if (count == 0) {
                Admin admin = new Admin();
                admin.setUsername(username);
                admin.setPassword(username);
                em.persist(admin);
            }
        }
    }
}