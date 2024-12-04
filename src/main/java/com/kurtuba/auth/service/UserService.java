package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.*;
import com.kurtuba.auth.data.model.dto.TokensDto;
import com.kurtuba.auth.data.model.dto.UserDto;
import com.kurtuba.auth.data.model.dto.UserRegistrationDto;
import com.kurtuba.auth.data.model.dto.UserRegistrationOtherProviderDto;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.data.repository.UserRoleRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.TokenUtils;
import com.kurtuba.auth.utils.Utils;
import com.nimbusds.jose.shaded.gson.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.kurtuba.auth.utils.Utils.generateValidationCode;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final UserTokenService userTokenService;

    private final EmailService emailService;

    private final EntityManagerFactory entityManagerFactory;

    private EntityManager em() {
        return entityManagerFactory.createEntityManager();
    }

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                       UserTokenService userTokenService, EmailService emailService, EntityManagerFactory entityManagerFactory) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
        this.entityManagerFactory = entityManagerFactory;
    }


    /**
     * Runs when sign in with Google
     *
     * @param username
     */
    @Transactional
    public void processOAuthPostLogin(String username) {
        User existUser = userRepository.getUserByUsername(username);

        if (existUser == null) {
            User newUser = new User();
            newUser.setEmail(username);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setActivated(true);

            userRepository.save(newUser);

            System.out.println("Created new user: " + username);
        }

    }

    /**
     * Temporary method. Only user for local development. Will be removed
     *
     * @param user
     */
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }

    public User getUserByUsernameOrEmail(String email) {
        return userRepository.getUserByEmailOrUsername(email);
    }


    /**
     * Used by both authorization server login(CustomAuthenticationProvider) and
     * custom rest request login(EmailPassLoginController)
     * <p>
     * transaction is managed manually because we may save data to db and then throw BusinessLogicException
     */


    public User authenticate(String emailUsername, String pass) {
        User user = userRepository.getUserByEmailOrUsername(emailUsername);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);
        }
        long timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
        if (user.isLocked() && LocalDateTime.now().isBefore(user.getLastLoginAttempt().plusMinutes(timeToWait))) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(), "Account locked until " + user.getLastLoginAttempt().plusMinutes(timeToWait));
        }

        user.setLastLoginAttempt(LocalDateTime.now());
        String dbPass = user.getPassword();
        EntityTransaction transaction = null;
        try (EntityManager em = em()) {
            transaction = em.getTransaction();
            transaction.begin();
            user = em.find(User.class,user.getId());
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

    @Transactional
    public TokensDto generateTokensForLoginByRestRequest(String emailUsername, String pass, ClientType clientType) {

        User user = authenticate(emailUsername, pass);

        return userTokenService.createAndSaveTokens(user.getId(), clientType);
    }

    @Transactional
    public UserDto register(@Valid UserRegistrationDto newUser) {
        if (userRepository.getUserByEmail(newUser.getEmail()) != null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.getUserByUsername(newUser.getUsername()) != null) {
            throw new BusinessLogicException(ErrorEnum.USER_USERNAME_ALREADY_EXISTS);
        }

        String pass = newUser.getPassword();
        newUser.setPassword(new BCryptPasswordEncoder().encode(pass));

        User user = newUser.toUser();
        user.setCanChangeUsername(false);

        user.setEmailValidationCode(String.valueOf(generateValidationCode()));
        userRepository.save(user);
        user.setUserRoles(List.of(
                UserRole.builder()
                        .userId(user.getId())
                        .role(AuthoritiesEnum.USER)
                        .build()));
        userRoleRepository.saveAll(user.getUserRoles());

        try {
            emailService.sendValidationCodeMail(user.getEmail(), user.getEmailValidationCode());
        } catch (BusinessLogicException e) {
            //TODO let email scheduler handle
            e.printStackTrace();
        }

        return UserDto.fromUser(user);
    }

    @Transactional
    public UserDto validateEmail(@NotEmpty String email, @NotEmpty String code) {
        User user = userRepository.getUserByEmailAndEmailValidatedIsFalseAndEmailValidationCodeIsNotNull(email);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_STATUS_INVALID);
        }
        if (user.getEmailValidationCode().equals(code)) {
            user.setEmailValidated(true);
            return UserDto.fromUser(userRepository.save(user));
        } else {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_CODE_INVALID);
        }
    }

    @Transactional
    public void resendValidationCode(@NotEmpty String email) {
        User user = userRepository.getUserByEmailAndEmailValidatedIsFalseAndEmailValidationCodeIsNotNull(email);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_STATUS_INVALID);
        }
        user.setEmailValidationCode(String.valueOf(generateValidationCode()));
        userRepository.save(user);
        try {
            emailService.sendValidationCodeMail(user.getEmail(), user.getEmailValidationCode());
        } catch (BusinessLogicException e) {
            //TODO let email scheduler handle
        }

    }

    @Transactional
    public UserRegistrationDto registerByAnotherProvider(@Valid UserRegistrationOtherProviderDto newUserByOtherProvider) {

        UserRegistrationDto decodedUser = null;
        if (newUserByOtherProvider.getProvider().equals(AuthProvider.GOOGLE)) {
            try {
                decodedUser = TokenUtils.decodeGoogleToken(newUserByOtherProvider.getToken(), newUserByOtherProvider.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessLogicException(ErrorEnum.USER_OTHER_PROVIDER_INVALID_TOKEN);
            }
        }
        if (newUserByOtherProvider.getProvider().equals(AuthProvider.FACEBOOK)) {
            try {

                JsonObject jsonUser = TokenUtils.decodeTokenPayload(newUserByOtherProvider.getToken());
                decodedUser = new UserRegistrationDto();
                decodedUser.setEmail(jsonUser.get("email").getAsString());
                decodedUser.setName(jsonUser.get("given_name").getAsString());
                decodedUser.setSurname(jsonUser.get("family_name").getAsString());
                decodedUser.setAuthProvider(AuthProvider.FACEBOOK);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessLogicException(ErrorEnum.USER_OTHER_PROVIDER_INVALID_TOKEN);
            }
        }

        //else check twitter etc. then...

        //check if user already exists
        User existingUser = userRepository.getUserByEmail(decodedUser.getEmail());

        if (existingUser == null) {
            //this user never existed, let make one and return a token
            User user = decodedUser.toUser();
            user.setEmailValidated(true);
            String pass = UUID.randomUUID().toString();
            user.setPassword(new BCryptPasswordEncoder().encode(pass));
            String provisionalUsername = user.getEmail().split("@")[0];
            if (provisionalUsername.length() > 25) {
                provisionalUsername = provisionalUsername.substring(0, 25);
            }
            user.setUsername(provisionalUsername + "." + Utils.generateRandomAlphanumericString(6));
            user.setCanChangeUsername(true);
            userRepository.save(user);
            user.setUserRoles(List.of(
                    UserRole.builder()
                            .userId(user.getId())
                            .role(AuthoritiesEnum.USER)
                            .build()));
            userRoleRepository.saveAll(user.getUserRoles());
            decodedUser.setPassword(pass);
            return decodedUser;
        }

        if (existingUser.getAuthProvider().equals(AuthProvider.KURTUBA)) {
            //this is a regular user and we cannot return a token without changing pass so have to log in properly. Throw error
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        if (existingUser.getAuthProvider().equals(newUserByOtherProvider.getProvider())) {
            //this email with given provider exists. check active, lock etc fields and return a token
            if (existingUser.isActivated() && !existingUser.isLocked()) {
                String pass = UUID.randomUUID().toString();
                existingUser.setPassword(new BCryptPasswordEncoder().encode(pass));
                decodedUser.setPassword(pass);
                userRepository.save(existingUser);
                return decodedUser;//accessTokenUtil.getAccessToken(existingUser.getEmail(), pass);
            }

        }

        //This email with different provider exists. Check active, lock etc fields and return a token
        //That also means as long as user uses other providers with same email, same user will be logged in
        String pass = UUID.randomUUID().toString();
        existingUser.setPassword(new BCryptPasswordEncoder().encode(pass));
        existingUser.setAuthProvider(decodedUser.getAuthProvider());
        decodedUser.setPassword(pass);
        userRepository.save(existingUser);
        return decodedUser;//accessTokenUtil.getAccessToken(existingUser.getEmail(), pass);

    }


    public UserDto getUserByEmail(String email) {
        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.RESOURCE_NOT_FOUND);
        }
        return UserDto.fromUser(user);
    }

    public UserDto getUserById(String id) {
        User user = userRepository.getUserById(id);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.RESOURCE_NOT_FOUND);
        }
        return UserDto.fromUser(user);
    }

    public boolean isUsernameAvailable(String username) {
        return userRepository.getUserByUsername(username) == null;
    }

    public boolean isEmailAvailable(String email) {
        return userRepository.getUserByEmail(email) == null;
    }

}
