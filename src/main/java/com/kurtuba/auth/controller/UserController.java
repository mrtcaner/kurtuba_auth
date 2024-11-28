package com.kurtuba.auth.controller;


import com.kurtuba.auth.service.UserService;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    UserService userService;

    //todo this method must be for internal use only.
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity getUserById(@PathVariable @NotEmpty String id, JwtAuthenticationToken principal) {
        if(principal == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).body("");
        }
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(id));
    }

    //todo only certain info must be shared through a DTO
    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity getUserInfo(JwtAuthenticationToken principal) {
        if(principal == null){
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).body("");
        }
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(principal.getName()));
    }
}
