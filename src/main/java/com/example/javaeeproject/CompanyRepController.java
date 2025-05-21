package com.example.javaeeproject;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@RequestScoped
@Path("/rep")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CompanyRepController {
    @Inject
    private CompanyRepresentativeService service;

    @POST
    @Path("/login")
    public Response login(Map<String, String> creds) {
        CompanyRepresentative rep = service.login(creds.get("companyName"), creds.get("password"));
        return Response.ok(rep).build();
    }

    @GET
    @Path("/dishes/{repId}")
    public Response getCurrentDishes(@PathParam("repId") Long repId) {
        return Response.ok(service.getCurrentDishes(repId)).build();
    }


    @GET
    @Path("/sales/{repId}")
    public Response getSales(@PathParam("repId") Long repId) {
        return Response.ok(service.getSoldDishesWithCustomerAndShipping(repId)).build();
    }

    @POST
    @Path("/dishes")
    public Response addDish(Dish dish) {
        service.addDish(dish);
        return Response.ok("Dish added").build();
    }

    @PUT
    @Path("/dishes")
    public Response updateDish(Dish dish) {
        service.updateDish(dish);
        return Response.ok("Dish updated").build();
    }
}
