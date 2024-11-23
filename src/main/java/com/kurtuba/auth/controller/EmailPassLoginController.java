package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.model.dto.LoginCredentialsDto;
import com.kurtuba.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class EmailPassLoginController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity login(@Valid @RequestBody LoginCredentialsDto loginCredentials) {
        //throws exception if authentication fails
        userService.authenticate(loginCredentials.getEmailUsername(), loginCredentials.getPass());
        //no exception means successful authentication. Generate token and return
        return ResponseEntity.status(HttpStatus.OK).body(userService.generateAccessTokenForLoginByRestRequest(loginCredentials.getEmailUsername(), loginCredentials.getClientType()));


    }
}
