package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.model.dto.UserDto;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("auth")
public class AdminController {

    @GetMapping("/admin/user/detail")
    public UserDto getUserDetails(@RequestParam @NotEmpty String userNameEmail){
        return UserDto.builder().build();
    }

}
