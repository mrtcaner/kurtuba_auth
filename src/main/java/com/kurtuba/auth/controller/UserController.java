package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.model.JWTClaimsEnum;
import com.kurtuba.auth.data.model.RoleEnum;
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

    final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * this method is for internal use only
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity getUserById(@PathVariable @NotEmpty String id, JwtAuthenticationToken principal) {
        if(!isServiceRequest(principal)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).body("");
        }
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(id));
    }

    /**
     * users with a valid token can access
     * todo only certain info must be shared through a DTO
     * @param principal
     * @return
     */
    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity getUserInfo(JwtAuthenticationToken principal) {
        if(principal == null){
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).body("");
        }
        if(isServiceRequest(principal)){
            // SERVICEs are not users
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).body("");
        }
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(principal.getName()));
    }

    private boolean isServiceRequest(JwtAuthenticationToken principal){
        // only authenticated users are allowed
        if(principal == null){
            return false;
        }
        // token must have a role claim
        if(principal.getTokenAttributes().get(JWTClaimsEnum.ROLE.getDisplayName()) == null){
            return false;
        }
        // role has to be "SERVICE"
        if(!principal.getTokenAttributes().get(JWTClaimsEnum.ROLE.getDisplayName()).equals(RoleEnum.SERVICE.name())){
            return false;
        }

        return true;
    }
}
