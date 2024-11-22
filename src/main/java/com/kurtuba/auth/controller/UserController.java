package com.kurtuba.auth.controller;


import com.kurtuba.auth.service.UserService;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("auth")
public class UserController {

    @Autowired
    UserService userService;

    //todo this method must be for internal use only.
    @GetMapping("/user/{id}")
    @ResponseBody
    public ResponseEntity getUserById(@PathVariable @NotEmpty String id) {
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(id));
    }

    //todo only certain info must be shared through a DTO
    @GetMapping("/user/info")
    @ResponseBody
    public ResponseEntity getUserById(Principal principal) {
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserByUsernameOrEmail(principal.getName()));
    }
}
