package com.kurtuba.auth.config.provider;

import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    final
    UserDetailsService userDetailsService;

    final
    UserService userService;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService, UserService userService) {
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usernameEmail = authentication.getName();
        String password = authentication.getCredentials().toString();

        //Authenticate
        try{
            userService.authenticate(usernameEmail,password);
        }catch (BusinessLogicException e){
            throw new BadCredentialsException("Invalid credentials");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(usernameEmail);
        // Create a fully authenticated Authentication object
        return new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities());
    }
    @Override
    public boolean supports(Class<?> authentication) {
        // Return true if this AuthenticationProvider supports the provided authentication class
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
