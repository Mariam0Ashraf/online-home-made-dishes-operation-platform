package com.example.javaeeproject;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RequestScoped
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminController {
    @Inject
    private AdminService service;

    @POST
    @Path("/create-reps")
    public Response createReps(List<String> companyNames) {
        List<CompanyRepresentative> reps = service.createCompanyRepresentatives(companyNames);
        return Response.ok(reps).build();
    }

    @GET
    @Path("/customers")
    public Response getCustomers() {
        return Response.ok(service.listAllCustomers()).build();
    }

    @GET
    @Path("/reps")
    public Response getReps() {
        return Response.ok(service.listAllCompanyRepresentatives()).build();
    }
}
