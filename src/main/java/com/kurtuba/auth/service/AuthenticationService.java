package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
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

    final
    EntityManagerFactory entityManagerFactory;

    public AuthenticationService(EntityManagerFactory entityManagerFactory, UserService userService, UserTokenService userTokenService, RegisteredClientRepository registeredClientRepository) {
        this.userService = userService;
        this.userTokenService = userTokenService;
        this.registeredClientRepository = registeredClientRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    private EntityManager em() {
        return entityManagerFactory.createEntityManager();
    }

    @Transactional
    public TokensResponseDto authenticateAndGetTokens(String emailMobile, String pass,
                                                      String registeredClientId, String registeredClientSecret) {
        // authenticate user and get tokens
        return userTokenService.validateRegisteredClientAndGetTokens(authenticate(emailMobile, pass), registeredClientId, registeredClientSecret);


    }

    /**
     * transaction is managed manually because we may save data to db and then throw BusinessLogicException
     */
    public User authenticate(String emailMobile, String pass) {
        User user = userService.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS));

        long timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
        if (user.isLocked() && LocalDateTime.now().isBefore(user.getLastLoginAttempt().plusMinutes(timeToWait))) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(), "Account locked until " + user.getLastLoginAttempt().plusMinutes(timeToWait));
        }

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_ACTIVATION_NOT_ACTIVATED);
        }

        user.setLastLoginAttempt(LocalDateTime.now());
        String dbPass = user.getPassword();
        EntityTransaction transaction = null;
        try (EntityManager em = em()) {
            transaction = em.getTransaction();
            transaction.begin();
            user = em.find(User.class, user.getId());
            // check password
            if (!BCrypt.checkpw(pass, dbPass)) {
                user.setFailedLoginCount(user.getFailedLoginCount() + 1);

                if (user.getFailedLoginCount() >= 5) {
                    user.setShowCaptcha(true);
                }

                if (user.getFailedLoginCount() >= 10) {
                    user.setLocked(true);
                    em.persist(user);
                    transaction.commit();
                    timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
                    throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(), "Account locked until " + user.getLastLoginAttempt().plusMinutes(timeToWait));
                }
                em.persist(user);
                transaction.commit();
                throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);

            }
            user.setFailedLoginCount(0);
            user.setShowCaptcha(false);
            user.setLocked(false);
            em.persist(user);
            transaction.commit();
            return user;

        }

    }


}
