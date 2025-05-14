package com.example.javaeeproject;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@RequestScoped
@Path("/customer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerController {
    @Inject
    private CustomerService service;

    @POST
    @Path("/register")
    public Response register(Customer customer) {
        service.register(customer);
        return Response.ok("Customer registered").build();
    }

    @POST
    @Path("/login")
    public Response login(Map<String, String> creds) {
        Customer customer = service.login(creds.get("email"), creds.get("password"));
        return Response.ok(customer).build();
    }

    @GET
    @Path("/orders/{customerId}")
    public Response getOrders(@PathParam("customerId") Long customerId) {
        return Response.ok(service.getCurrentAndPastOrders(customerId)).build();
    }

    @POST
    @Path("/order/{customerId}")
    public Response placeOrder(@PathParam("customerId") Long customerId, List<OrderItem> items) {
        Order order = service.placeOrder(customerId, items);
        return Response.ok(order).build();
    }

    @PUT
    @Path("/order/{id}/confirm")
    public Response confirm(@PathParam("id") Long id) {
        service.confirmOrder(id);
        return Response.ok("Order confirmed").build();
    }
}
