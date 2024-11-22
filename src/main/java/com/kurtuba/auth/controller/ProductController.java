package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.model.Product;
import com.kurtuba.auth.utils.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("auth")
public class ProductController {

    @Autowired
    private AuthenticationFacade user;
 
    @GetMapping("/products")
    public List<Product> getProducts() {
        System.out.println("user:" + user.getAuthentication().getName());
        return Arrays.asList(
                new Product[]{new Product(1, "I Pad", 10),
                        new Product(2, "I Phone", 12),
                        new Product(3, "MacBook", 15)});
    }
}