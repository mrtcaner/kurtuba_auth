package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    final
    UserService userService;

    // must create a new transaction and persist it in case of BusinessLogicException
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessLogicException.class)
    public User authenticate(String emailMobile, String pass) throws BusinessLogicException {
        User user = userService.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS));

        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        // first do account lock and timing check
        long timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 5;// is 0 until
        // 10 failed attempts
        if (user.isLocked() && Instant.now().isBefore(user.getLastLoginAttempt().plus(Duration.ofMinutes(timeToWait)))) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(),
                                             "blockedUntil:" + user.getLastLoginAttempt().plus(Duration.ofMinutes(timeToWait)));
        }

        user.setLastLoginAttempt(Instant.now());
        String dbPass = user.getPassword();
        // check password
        if (!BCrypt.checkpw(pass, dbPass)) {
            // incorrect password
            user.setFailedLoginCount(user.getFailedLoginCount() + 1);

            if (user.getFailedLoginCount() >= 5) {
                //todo: for web, currently not implemented
                user.setShowCaptcha(true);
            }

            if (user.getFailedLoginCount() >= 10) {
                user.setLocked(true);
                userService.saveUser(user);
                timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
                throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(),
                                                 "blockedUntil:" + user.getLastLoginAttempt().plus(Duration.ofMinutes(timeToWait)));
            }
            userService.saveUser(user);
            throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);

        }
        // doing activation check here to avoid timing attacks
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
