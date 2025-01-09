package com.kurtuba.auth.config.provider;

import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.dto.KurtubaUserDetailsDto;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    final
    UserService userService;

    public CustomAuthenticationProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String emailMobile = authentication.getName();
        String password = authentication.getCredentials().toString();

        //Authenticate
        try{
            userService.authenticate(emailMobile,password);
        }catch (BusinessLogicException e){
            throw new BadCredentialsException("Invalid credentials");
        }

        UserDetails userDetails = loadUserByUsername(emailMobile);
        // Create a fully authenticated Authentication object
        return new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities());
    }

    public UserDetails loadUserByUsername(String emailMobile)
            throws UsernameNotFoundException {
        User user = userService.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST)
        );

        List<SimpleGrantedAuthority> auths = new ArrayList<>();
        user.getUserRoles().stream().map(auth -> auths.add(new SimpleGrantedAuthority(auth.getRole().getName())));
        return KurtubaUserDetailsDto.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(auths)
                .locked(user.isLocked())
                .activated(user.isActivated())
                .build();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Return true if this AuthenticationProvider supports the provided authentication class
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
