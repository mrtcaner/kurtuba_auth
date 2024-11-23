package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.model.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("auth")
public class ProductController {

    @GetMapping("/products")
    public List<Product> getProducts(Principal principal) {
        if(principal != null && principal.getName() != null){
            System.out.println("ProductController.getProducts user:" + principal.getName());
        }else{
            System.out.println("ProductController.getProducts user: null");
        }
        return Arrays.asList(
                new Product[]{new Product(1, "I Pad", 10),
                        new Product(2, "I Phone", 12),
                        new Product(3, "MacBook", 15)});
    }
}