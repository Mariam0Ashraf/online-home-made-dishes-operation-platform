package com.example.javaeeproject;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestScoped
@Path("/customer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerController {
    @Inject
    private CustomerService service;

    @Path("/register")
    @POST
    public Response register(Customer customer) {
        service.register(customer);
        return Response.ok("Customer registered").build();
    }

    @POST
    @Path("/login")
    public Response login(Map<String, String> creds) {
        try {
            Customer customer = service.login(creds.get("email"), creds.get("password"));
            return Response.ok(customer).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid email or password").build();
        }
    }

    @GET
    @Path("/orders/{customerId}")
    public Response getOrders(@PathParam("customerId") Long customerId) {
        if (!service.isLoggedIn() || !customerId.equals(service.getLoggedInCustomerId())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be logged in as this customer").build();
        }
        return Response.ok(service.getCurrentAndPastOrders(customerId)).build();
    }

    @POST
    @Path("/order/{customerId}")
    public Response placeOrder(@PathParam("customerId") Long customerId, List<OrderItemDTO> itemDTOs) {
        // Convert DTOs to OrderItems
        List<OrderItem> items = itemDTOs.stream()
                .map(dto -> {
                    OrderItem item = new OrderItem();
                    item.setDishId(dto.getDishId());
                    item.setQuantity(dto.getQuantity());
                    return item;
                })
                .collect(Collectors.toList());

        if (!service.isLoggedIn() || !customerId.equals(service.getLoggedInCustomerId())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be logged in as this customer").build();
        }

        try {
            Order order = service.placeOrder(customerId, items);
            return Response.ok(order).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build();
        }
    }
    @GET
    @Path("/dishes")
    public Response getAllDishes() {
        if (!service.isLoggedIn()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be logged in").build();
        }
        return Response.ok(service.getAllActiveDishes()).build();
    }
}
