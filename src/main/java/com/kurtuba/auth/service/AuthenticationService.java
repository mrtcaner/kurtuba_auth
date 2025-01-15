package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthenticationService {

    final
    UserService userService;

    final
    UserTokenService userTokenService;

    final
    RegisteredClientRepository registeredClientRepository;


    public AuthenticationService(EntityManagerFactory entityManagerFactory, UserService userService, UserTokenService userTokenService, RegisteredClientRepository registeredClientRepository) {
        this.userService = userService;
        this.userTokenService = userTokenService;
        this.registeredClientRepository = registeredClientRepository;
    }

    // must create a new transaction and persist it in case of BusinessLogicException
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessLogicException.class)
    public User authenticate(String emailMobile, String pass) throws BusinessLogicException {
        User user = userService.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS));

        long timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
        if (user.isLocked() && LocalDateTime.now().isBefore(user.getLastLoginAttempt().plusMinutes(timeToWait))) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(), "Account locked until " + user.getLastLoginAttempt().plusMinutes(timeToWait));
        }

        user.setLastLoginAttempt(LocalDateTime.now());
        String dbPass = user.getPassword();
        // check password
        if (!BCrypt.checkpw(pass, dbPass)) {
            user.setFailedLoginCount(user.getFailedLoginCount() + 1);

            if (user.getFailedLoginCount() >= 5) {
                user.setShowCaptcha(true);
            }

            if (user.getFailedLoginCount() >= 10) {
                user.setLocked(true);
                userService.saveUser(user);
                timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
                throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(), "Account locked until " + user.getLastLoginAttempt().plusMinutes(timeToWait));
            }
            userService.saveUser(user);
            throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);

        }
        // password matched but is account activated?
        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_ACTIVATION_NOT_ACTIVATED);
        }

        user.setFailedLoginCount(0);
        user.setShowCaptcha(false);
        user.setLocked(false);
        userService.saveUser(user);
        return user;

    }


}
