package com.example.javaeeproject;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Startup
@Singleton
public class AdminInitializer {

    @Inject
    private AdminService adminService;

    @PostConstruct
    public void init() {
        adminService.initializeAdmins();
        System.out.println("Admins initialized at server startup.");

    }
}
